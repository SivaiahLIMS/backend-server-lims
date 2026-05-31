package com.sivayahealth.lims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles the analyst-facing worksheet execution flow:
 *   - Render template (blocks + slots) for a worksheet
 *   - Save / upsert a field value
 *   - Compute formula result for a test case once all slots are filled
 *   - Review a test case result (pass/fail + comments)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetDocumentService {

    private final WorksheetMasterRepository        worksheetRepo;
    private final DocumentTestCaseRepository       testCaseRepo;
    private final DocumentTemplateBlockRepository  blockRepo;
    private final DocumentFieldSlotRepository      slotRepo;
    private final WorksheetFieldValueRepository    fieldValueRepo;
    private final WorksheetTestCaseResultRepository resultRepo;
    private final AppUserRepository                userRepo;
    private final WorksheetValidationService       validationService;

    private final ObjectMapper objectMapper;

    // ── Template rendering ────────────────────────────────────────────────────

    /**
     * Returns the full document structure for a worksheet — ordered test cases,
     * each with ordered blocks and their field slots. Also includes any saved
     * field values so the analyst UI can pre-populate inputs.
     */
    @Transactional(readOnly = true)
    public WorksheetTemplateView getTemplateView(Long tenantId, Long branchId, Long worksheetId) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, branchId, worksheetId);
        if (worksheet.getDocumentVersion() == null) {
            throw LimsException.badRequest("Worksheet has no document version linked");
        }
        Long versionId = worksheet.getDocumentVersion().getId();

        List<DocumentTestCase> testCases =
                testCaseRepo.findByDocumentVersion_IdOrderByTestCaseIndexAsc(versionId);

        List<TestCaseView> tcViews = new ArrayList<>();
        for (DocumentTestCase tc : testCases) {
            List<DocumentTemplateBlock> blocks =
                    blockRepo.findByTestCase_TestCaseIdOrderByBlockIndexAsc(tc.getTestCaseId());
            List<DocumentFieldSlot> slots =
                    slotRepo.findByTestCase_TestCaseIdOrderByFieldIndexAsc(tc.getTestCaseId());
            List<WorksheetFieldValue> values =
                    fieldValueRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                            worksheetId, tc.getTestCaseId());

            Optional<WorksheetTestCaseResult> result =
                    resultRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                            worksheetId, tc.getTestCaseId());

            tcViews.add(new TestCaseView(tc, blocks, slots, values, result.orElse(null)));
        }

        return new WorksheetTemplateView(worksheet, tcViews);
    }

    // ── Field value save / upsert ─────────────────────────────────────────────

    /**
     * Save or update a single field slot value for an analyst.
     * Supports upsert: if the analyst already filled this slot, updates it.
     */
    @Transactional
    public WorksheetFieldValue saveFieldValue(Long tenantId, Long branchId,
                                               Long worksheetId, Long userId,
                                               Long slotId,
                                               BigDecimal numericValue,
                                               String unit,
                                               String qualifier,
                                               String comment) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, branchId, worksheetId);
        if (!"IN_PROGRESS".equals(worksheet.getStatus()) && !"DRAFT".equals(worksheet.getStatus())) {
            throw LimsException.badRequest(
                    "Fields can only be filled when worksheet is DRAFT or IN_PROGRESS");
        }
        AppUser analyst = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        DocumentFieldSlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> LimsException.notFound("Field slot not found"));

        Optional<WorksheetFieldValue> existing =
                fieldValueRepo.findByWorksheet_WorksheetIdAndSlot_SlotId(worksheetId, slotId);

        WorksheetFieldValue fv = existing.orElseGet(() -> WorksheetFieldValue.builder()
                .worksheet(worksheet)
                .slot(slot)
                .testCase(slot.getTestCase())
                .tenant(worksheet.getTenant())
                .branch(worksheet.getBranch())
                .build());

        fv.setNumericValue(numericValue);
        fv.setUnit(unit);
        fv.setQualifier(qualifier != null ? qualifier : "EXACT");
        fv.setComment(comment);

        if (existing.isEmpty()) {
            fv.setEnteredBy(analyst);
            fv.setEnteredAt(LocalDateTime.now());
        } else {
            fv.setModifiedBy(analyst);
            fv.setModifiedAt(LocalDateTime.now());
        }

        return fieldValueRepo.save(fv);
    }

    // ── Formula computation ───────────────────────────────────────────────────

    /**
     * Evaluates the formula for a test case by substituting all slot values
     * and computing the result. Stores it in worksheet_test_case_result.
     *
     * Requires all slots to be filled; throws if any are missing.
     */
    @Transactional
    public WorksheetTestCaseResult computeResult(Long tenantId, Long branchId,
                                                  Long worksheetId,
                                                  Long testCaseId,
                                                  Long userId,
                                                  String resultUnit) {
        WorksheetMaster worksheet = loadWorksheet(tenantId, branchId, worksheetId);
        AppUser analyst = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        DocumentTestCase testCase = testCaseRepo.findById(testCaseId)
                .orElseThrow(() -> LimsException.notFound("Test case not found"));

        List<DocumentFieldSlot> slots =
                slotRepo.findByTestCase_TestCaseIdOrderByFieldIndexAsc(testCaseId);
        List<WorksheetFieldValue> values =
                fieldValueRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                        worksheetId, testCaseId);

        // Map slotId → value
        Map<Long, BigDecimal> slotValueMap = new HashMap<>();
        for (WorksheetFieldValue v : values) {
            if (v.getNumericValue() != null) {
                slotValueMap.put(v.getSlot().getSlotId(), v.getNumericValue());
            }
        }

        // Check all slots are filled
        List<String> missing = new ArrayList<>();
        for (DocumentFieldSlot slot : slots) {
            if (!slotValueMap.containsKey(slot.getSlotId())) {
                missing.add(slot.getFieldVariable() + " (" + slot.getLabel() + ")");
            }
        }
        if (!missing.isEmpty()) {
            throw LimsException.badRequest("Missing values for: " + String.join(", ", missing));
        }

        // Build variable → value map using field_variable (A, B, C...)
        Map<String, BigDecimal> varValues = new HashMap<>();
        for (DocumentFieldSlot slot : slots) {
            varValues.put(slot.getFieldVariable(), slotValueMap.get(slot.getSlotId()));
        }

        String expression = testCase.getFormulaExpression();
        if (expression == null || expression.isBlank()) {
            expression = testCase.getFormulaText();
        }

        String substituted = substituteExpression(expression, varValues);
        BigDecimal computed = evaluate(substituted);

        Optional<WorksheetTestCaseResult> existing =
                resultRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                        worksheetId, testCaseId);

        WorksheetTestCaseResult result = existing.orElseGet(() ->
                WorksheetTestCaseResult.builder()
                        .worksheet(worksheet)
                        .testCase(testCase)
                        .tenant(worksheet.getTenant())
                        .branch(worksheet.getBranch())
                        .build());

        result.setFormulaSubstituted(substituted);
        result.setComputedResult(computed);
        result.setResultUnit(resultUnit);
        result.setPassFail("PENDING");
        result.setComputedBy(analyst);
        result.setComputedAt(LocalDateTime.now());

        WorksheetTestCaseResult saved = resultRepo.save(result);

        // Validate the final computed result against the formula slot's OOS/OOT rule
        DocumentFieldSlot formulaSlot = slots.stream()
                .filter(s -> "FORMULA".equalsIgnoreCase(s.getFieldType()))
                .findFirst().orElse(null);
        if (formulaSlot != null && computed != null) {
            validationService.validateComputedResult(
                    worksheetId, formulaSlot.getSlotId(), computed, resultUnit, userId);
        }

        return saved;
    }

    // ── Result review ─────────────────────────────────────────────────────────

    /**
     * Reviewer marks a test case result as PASS or FAIL with optional comments.
     */
    @Transactional
    public WorksheetTestCaseResult reviewResult(Long tenantId, Long branchId,
                                                 Long worksheetId,
                                                 Long testCaseId,
                                                 Long userId,
                                                 String passFail,
                                                 String comments) {
        loadWorksheet(tenantId, branchId, worksheetId);
        AppUser reviewer = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        WorksheetTestCaseResult result =
                resultRepo.findByWorksheet_WorksheetIdAndTestCase_TestCaseId(
                                worksheetId, testCaseId)
                        .orElseThrow(() -> LimsException.notFound(
                                "No computed result found for this test case"));

        if (!Set.of("PASS", "FAIL").contains(passFail)) {
            throw LimsException.badRequest("passFail must be PASS or FAIL");
        }

        result.setPassFail(passFail);
        result.setReviewedBy(reviewer);
        result.setReviewedAt(LocalDateTime.now());
        result.setReviewComments(comments);
        return resultRepo.save(result);
    }

    // ── Expression evaluation ─────────────────────────────────────────────────

    /**
     * Substitutes variable names (A, B, C...) with their numeric values
     * in the formula expression string.
     */
    private String substituteExpression(String expression, Map<String, BigDecimal> varValues) {
        // Sort by length descending to avoid AA being replaced by two A substitutions
        List<String> vars = varValues.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();

        String result = expression;
        for (String var : vars) {
            result = result.replaceAll("\\b" + Pattern.quote(var) + "\\b",
                    varValues.get(var).toPlainString());
        }
        return result;
    }

    /**
     * Simple arithmetic expression evaluator supporting +, -, *, /, (, ).
     * Uses a recursive descent parser — no external dependencies.
     */
    private BigDecimal evaluate(String expression) {
        try {
            // Strip non-numeric/operator characters that may remain from formula text
            String cleaned = expression.replaceAll("[^0-9+\\-*/().E]", " ").trim();
            ExprParser parser = new ExprParser(cleaned);
            return parser.parse().setScale(6, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Formula evaluation failed for expression '{}': {}", expression, e.getMessage());
            throw LimsException.badRequest(
                    "Could not evaluate formula: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WorksheetMaster loadWorksheet(Long tenantId, Long branchId, Long worksheetId) {
        WorksheetMaster w = worksheetRepo.findById(worksheetId)
                .orElseThrow(() -> LimsException.notFound("Worksheet not found"));
        if (!w.getTenant().getId().equals(tenantId) || !w.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Worksheet not found");
        }
        return w;
    }

    // ── View records ──────────────────────────────────────────────────────────

    public record WorksheetTemplateView(
            WorksheetMaster worksheet,
            List<TestCaseView> testCases) {}

    public record TestCaseView(
            DocumentTestCase testCase,
            List<DocumentTemplateBlock> blocks,
            List<DocumentFieldSlot> slots,
            List<WorksheetFieldValue> values,
            WorksheetTestCaseResult result) {}

    // ── Arithmetic expression parser ──────────────────────────────────────────

    private static class ExprParser {
        private final String   input;
        private       int      pos = 0;

        ExprParser(String input) { this.input = input.trim(); }

        BigDecimal parse() { BigDecimal v = expr(); skipWs(); return v; }

        private BigDecimal expr() {
            BigDecimal v = term();
            while (pos < input.length()) {
                skipWs();
                char c = peek();
                if (c == '+') { pos++; v = v.add(term()); }
                else if (c == '-') { pos++; v = v.subtract(term()); }
                else break;
            }
            return v;
        }

        private BigDecimal term() {
            BigDecimal v = factor();
            while (pos < input.length()) {
                skipWs();
                char c = peek();
                if (c == '*') { pos++; v = v.multiply(factor()); }
                else if (c == '/') {
                    pos++;
                    BigDecimal divisor = factor();
                    if (divisor.compareTo(BigDecimal.ZERO) == 0)
                        throw new ArithmeticException("Division by zero");
                    v = v.divide(divisor, 10, RoundingMode.HALF_UP);
                } else break;
            }
            return v;
        }

        private BigDecimal factor() {
            skipWs();
            if (pos >= input.length())
                throw new RuntimeException("Unexpected end of expression");
            char c = peek();
            if (c == '(') {
                pos++;
                BigDecimal v = expr();
                skipWs();
                if (pos < input.length() && peek() == ')') pos++;
                return v;
            }
            if (c == '-') { pos++; return factor().negate(); }
            return number();
        }

        private BigDecimal number() {
            skipWs();
            int start = pos;
            if (pos < input.length() && (peek() == '-' || peek() == '+')) pos++;
            while (pos < input.length() && (Character.isDigit(peek()) || peek() == '.' || peek() == 'E' || peek() == 'e')) pos++;
            String num = input.substring(start, pos).trim();
            if (num.isEmpty()) throw new RuntimeException("Expected number at position " + start);
            return new BigDecimal(num);
        }

        private void skipWs() { while (pos < input.length() && input.charAt(pos) == ' ') pos++; }
        private char peek() { return input.charAt(pos); }
    }
}
