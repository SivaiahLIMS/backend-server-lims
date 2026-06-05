package com.sivayahealth.lims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a DOCX file and persists the structure as:
 *   DocumentVersion → DocumentTestCase[] → DocumentTemplateBlock[] → DocumentFieldSlot[]
 *
 * Document segmentation rules:
 *   - Blocks are collected in order (PARAGRAPH, TABLE, IMAGE).
 *   - A paragraph whose text matches the FORMULA pattern ends the current test case.
 *   - Each test case MUST end with exactly one FORMULA block.
 *   - `--` occurrences in any non-formula block become DocumentFieldSlot rows.
 *   - Variables are assigned A, B, C... within each test case in left-to-right,
 *     top-to-bottom reading order.
 *   - In the formula text, any `--` is also replaced with A/B/C to build
 *     formula_expression.
 *
 * Formula detection: a paragraph is a formula block when its trimmed text starts
 * with "Formula" (case-insensitive) or contains "=[expression]" after the
 * FORMULA_LINE_PATTERN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocxParserService {

    private static final Pattern PLACEHOLDER   = Pattern.compile("--");
    private static final Pattern FORMULA_LINE  = Pattern.compile(
            "^(?i)(formula|result|assay|content|purity|potency|yield)\\b.*");
    private static final Pattern UNIT_HINT     = Pattern.compile(
            "(?i)\\b(ml|litre|liter|L|g|gram|kg|kilogram|mg|milligram|µg|mcg|mEq|IU|%|percent)\\b");

    private final ObjectMapper                    objectMapper;
    private final DocumentTestCaseRepository      testCaseRepo;
    private final DocumentTemplateBlockRepository blockRepo;
    private final DocumentFieldSlotRepository     slotRepo;
    private final TenantRepository                tenantRepo;
    private final BranchRepository                branchRepo;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Parse the DOCX stream and persist all test cases, blocks, and field slots
     * for the given document version. Idempotent — clears any prior data first.
     */
    @Transactional
    public ParseResult parse(InputStream docxStream, DocumentVersion version) {
        clearExistingData(version.getId());

        Tenant tenant = tenantRepo.findById(version.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + version.getTenantId()));
        Branch branch = branchRepo.findById(version.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found: " + version.getBranchId()));

        List<RawTestCase> rawTestCases = extractRawTestCases(docxStream);
        if (rawTestCases.isEmpty()) {
            throw new RuntimeException(
                "No test cases found. Each test case must end with a Formula paragraph.");
        }

        int savedTestCases  = 0;
        int savedBlocks     = 0;
        int savedSlots      = 0;

        for (int tcIdx = 0; tcIdx < rawTestCases.size(); tcIdx++) {
            RawTestCase raw = rawTestCases.get(tcIdx);

            // Build variable→slot mapping for this test case
            List<SlotDraft> slotDrafts = collectSlots(raw.contentBlocks());
            Map<Integer, String> slotVarMap = buildVariableMap(slotDrafts);

            // Build formula_expression by substituting -- with variable names
            String formulaExpression = buildFormulaExpression(raw.formulaText(), slotVarMap, slotDrafts);

            DocumentTestCase testCase = testCaseRepo.save(DocumentTestCase.builder()
                    .documentVersion(version)
                    .tenant(tenant)
                    .branch(branch)
                    .testCaseIndex(tcIdx + 1)
                    .testCaseName(deriveTestCaseName(raw, tcIdx + 1))
                    .formulaText(raw.formulaText())
                    .formulaExpression(formulaExpression)
                    .build());
            savedTestCases++;

            // Persist blocks in order
            int blockIndex = 0;
            for (RawBlock rb : raw.contentBlocks()) {
                DocumentTemplateBlock block = blockRepo.save(DocumentTemplateBlock.builder()
                        .testCase(testCase)
                        .documentVersion(version)
                        .tenant(tenant)
                        .branch(branch)
                        .blockIndex(blockIndex)
                        .blockType(rb.type())
                        .contentJson(rb.contentJson())
                        .storagePath(rb.storagePath())
                        .build());
                savedBlocks++;
                blockIndex++;

                // Persist slots for this block
                for (SlotDraft sd : slotDrafts) {
                    if (sd.rawBlock() == rb) {
                        slotRepo.save(DocumentFieldSlot.builder()
                                .testCase(testCase)
                                .documentVersion(version)
                                .block(block)
                                .tenant(tenant)
                                .branch(branch)
                                .fieldIndex(sd.fieldIndex())
                                .blockLocalIndex(sd.blockLocalIndex())
                                .fieldVariable(slotVarMap.get(sd.fieldIndex()))
                                .label(sd.label())
                                .defaultUnit(sd.defaultUnit())
                                .build());
                        savedSlots++;
                    }
                }
            }

            // Persist the FORMULA block itself (last in test case)
            blockRepo.save(DocumentTemplateBlock.builder()
                    .testCase(testCase)
                    .documentVersion(version)
                    .tenant(tenant)
                    .branch(branch)
                    .blockIndex(blockIndex)
                    .blockType("FORMULA")
                    .contentJson(toTextJson(raw.formulaText()))
                    .build());
            savedBlocks++;
        }

        return new ParseResult(savedTestCases, savedBlocks, savedSlots);
    }

    /**
     * Legacy parse used by existing DocumentService / endpoints that only need
     * fields + formulas as a JSON structure (not persisted).
     */
    public LegacyParseResult parseLegacy(InputStream docxStream) {
        List<RawTestCase> rawTestCases = extractRawTestCases(docxStream);
        List<Map<String, Object>> fields   = new ArrayList<>();
        List<Map<String, Object>> formulas = new ArrayList<>();
        ArrayNode sections = objectMapper.createArrayNode();

        for (RawTestCase tc : rawTestCases) {
            ObjectNode section = objectMapper.createObjectNode();
            section.put("section", tc.formulaText());
            ArrayNode sectionFields = objectMapper.createArrayNode();

            List<SlotDraft> slots = collectSlots(tc.contentBlocks());
            Map<Integer, String> varMap = buildVariableMap(slots);

            for (SlotDraft sd : slots) {
                Map<String, Object> f = new LinkedHashMap<>();
                f.put("name",         varMap.get(sd.fieldIndex()));
                f.put("label",        sd.label());
                f.put("type",         "number");
                f.put("required",     true);
                f.put("defaultValue", null);
                f.put("defaultUnit",  sd.defaultUnit());
                fields.add(f);
                sectionFields.add(objectMapper.valueToTree(f));
            }

            Map<String, Object> formula = new LinkedHashMap<>();
            formula.put("fieldName",  "result_tc" + tc.testCaseIndex());
            formula.put("expression", buildFormulaExpression(tc.formulaText(), varMap, slots));
            formulas.add(formula);

            section.set("fields", sectionFields);
            sections.add(section);
        }

        return new LegacyParseResult(fields, formulas, sections);
    }

    // ── Core extraction ───────────────────────────────────────────────────────

    private List<RawTestCase> extractRawTestCases(InputStream docxStream) {
        List<RawTestCase> result = new ArrayList<>();

        try (XWPFDocument doc = new XWPFDocument(docxStream)) {
            List<RawBlock> pending = new ArrayList<>();
            int testCaseIndex = 1;

            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph para) {
                    String text = para.getText().trim();
                    if (text.isEmpty()) continue;

                    if (isFormulaLine(text)) {
                        result.add(new RawTestCase(
                                new ArrayList<>(pending),
                                text,
                                testCaseIndex++));
                        pending.clear();
                    } else {
                        pending.add(new RawBlock("PARAGRAPH", toTextJson(text), null));
                    }

                } else if (element instanceof XWPFTable table) {
                    String tableJson = tableToJson(table);
                    pending.add(new RawBlock("TABLE", tableJson, null));
                }
            }

            // Any trailing blocks without a formula — wrap with empty formula
            if (!pending.isEmpty()) {
                log.warn("DocxParser: {} trailing blocks after last formula. " +
                        "Wrapping as test case with empty formula.", pending.size());
                result.add(new RawTestCase(
                        new ArrayList<>(pending),
                        "Formula: (no formula detected)",
                        testCaseIndex));
            }

        } catch (Exception e) {
            log.error("DOCX parsing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse DOCX: " + e.getMessage(), e);
        }

        return result;
    }

    // ── Slot collection ───────────────────────────────────────────────────────

    private List<SlotDraft> collectSlots(List<RawBlock> blocks) {
        List<SlotDraft> slots = new ArrayList<>();
        int fieldIndex = 1;

        for (RawBlock rb : blocks) {
            if ("PARAGRAPH".equals(rb.type())) {
                String text = extractText(rb.contentJson());
                int localIdx = 0;
                Matcher m = PLACEHOLDER.matcher(text);
                while (m.find()) {
                    String label     = extractLabel(text, m.start());
                    String unit      = detectUnit(text);
                    slots.add(new SlotDraft(rb, fieldIndex++, localIdx++, label, unit));
                }
            } else if ("TABLE".equals(rb.type())) {
                // Iterate cells in row-major order
                try {
                    ObjectNode node = (ObjectNode) objectMapper.readTree(rb.contentJson());
                    ArrayNode rows = (ArrayNode) node.get("rows");
                    if (rows != null) {
                        for (int r = 0; r < rows.size(); r++) {
                            ArrayNode cells = (ArrayNode) rows.get(r);
                            for (int c = 0; c < cells.size(); c++) {
                                String cellText = cells.get(c).asText();
                                int localIdx = 0;
                                Matcher m = PLACEHOLDER.matcher(cellText);
                                while (m.find()) {
                                    String label = extractLabel(cellText, m.start());
                                    String unit  = detectUnit(cellText);
                                    int globalLocal = r * 1000 + c * 100 + localIdx;
                                    slots.add(new SlotDraft(rb, fieldIndex++, globalLocal, label, unit));
                                    localIdx++;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Could not parse table JSON for slot extraction: {}", e.getMessage());
                }
            }
        }
        return slots;
    }

    // ── Variable naming ───────────────────────────────────────────────────────

    private Map<Integer, String> buildVariableMap(List<SlotDraft> slots) {
        Map<Integer, String> map = new LinkedHashMap<>();
        for (int i = 0; i < slots.size(); i++) {
            map.put(slots.get(i).fieldIndex(), indexToVariable(i));
        }
        return map;
    }

    private String indexToVariable(int idx) {
        if (idx < 26) return String.valueOf((char) ('A' + idx));
        // For more than 26 variables: AA, AB, ...
        return String.valueOf((char) ('A' + (idx / 26) - 1))
             + String.valueOf((char) ('A' + (idx % 26)));
    }

    // ── Formula expression builder ────────────────────────────────────────────

    private String buildFormulaExpression(String formulaText,
                                          Map<Integer, String> varMap,
                                          List<SlotDraft> slots) {
        String expr = formulaText;
        int localIdx = 0;
        StringBuffer sb = new StringBuffer();
        Matcher m = PLACEHOLDER.matcher(expr);
        while (m.find()) {
            String var = localIdx < slots.size()
                    ? varMap.getOrDefault(slots.get(localIdx).fieldIndex(), "?")
                    : "?";
            m.appendReplacement(sb, var);
            localIdx++;
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isFormulaLine(String text) {
        return FORMULA_LINE.matcher(text).matches();
    }

    private String toTextJson(String text) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("text", text);
        return node.toString();
    }

    private String extractText(String json) {
        try {
            return objectMapper.readTree(json).path("text").asText("");
        } catch (Exception e) {
            return "";
        }
    }

    private String tableToJson(XWPFTable table) {
        ArrayNode rows = objectMapper.createArrayNode();
        for (XWPFTableRow row : table.getRows()) {
            ArrayNode cells = objectMapper.createArrayNode();
            for (XWPFTableCell cell : row.getTableCells()) {
                cells.add(cell.getText().trim());
            }
            rows.add(cells);
        }
        ObjectNode node = objectMapper.createObjectNode();
        node.set("rows", rows);
        return node.toString();
    }

    private String extractLabel(String text, int dashPos) {
        // Take up to 60 chars before the -- as the label hint
        int start = Math.max(0, dashPos - 60);
        return text.substring(start, dashPos).replaceAll("[\\r\\n]+", " ").trim();
    }

    private String detectUnit(String text) {
        Matcher m = UNIT_HINT.matcher(text);
        if (m.find()) return normalizeUnit(m.group(1));
        return null;
    }

    private String normalizeUnit(String raw) {
        return switch (raw.toLowerCase()) {
            case "litre", "liter", "l" -> "L";
            case "gram", "g"            -> "g";
            case "kilogram", "kg"       -> "kg";
            case "milligram", "mg"      -> "mg";
            case "µg", "mcg"            -> "µg";
            case "meq"                  -> "mEq";
            case "iu"                   -> "IU";
            case "percent", "%"         -> "%";
            default                     -> raw;
        };
    }

    private String deriveTestCaseName(RawTestCase tc, int index) {
        // Try to extract a meaningful name from the first paragraph
        if (!tc.contentBlocks().isEmpty()) {
            RawBlock first = tc.contentBlocks().get(0);
            if ("PARAGRAPH".equals(first.type())) {
                String text = extractText(first.contentJson());
                if (!text.isBlank() && text.length() <= 120) {
                    return text.replaceAll("--", "[value]");
                }
            }
        }
        return "Test Case " + index;
    }

    private void clearExistingData(Long documentVersionId) {
        List<Long> testCaseIds = testCaseRepo
                .findByDocumentVersion_IdOrderByTestCaseIndexAsc(documentVersionId)
                .stream().map(DocumentTestCase::getTestCaseId).toList();

        for (Long tcId : testCaseIds) {
            slotRepo.deleteByTestCase_TestCaseId(tcId);
            blockRepo.deleteByTestCase_TestCaseId(tcId);
        }
        testCaseRepo.deleteByDocumentVersion_Id(documentVersionId);
    }

    // ── Records ───────────────────────────────────────────────────────────────

    public record ParseResult(int testCases, int blocks, int slots) {}

    public record LegacyParseResult(
            List<Map<String, Object>> fields,
            List<Map<String, Object>> formulas,
            ArrayNode sections) {}

    private record RawBlock(String type, String contentJson, String storagePath) {}

    private record RawTestCase(List<RawBlock> contentBlocks, String formulaText, int testCaseIndex) {}

    private record SlotDraft(RawBlock rawBlock, int fieldIndex, int blockLocalIndex,
                             String label, String defaultUnit) {}
}
