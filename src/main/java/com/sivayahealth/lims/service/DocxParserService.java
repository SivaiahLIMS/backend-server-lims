package com.sivayahealth.lims.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a DOCX file using Apache POI and extracts:
 *  - fields  : {name, label, type, required, defaultValue}
 *  - formulas: {fieldName, expression}
 *  - sections: heading-grouped field arrays
 *
 * Detection rules:
 *  - Paragraphs with style Heading1/Heading2 → section boundaries
 *  - Table cells containing [FIELD:name] → text input field
 *  - Table cells containing [NUMBER:name] → numeric field
 *  - Table cells containing [DATE:name]   → date field
 *  - Table cells containing [SELECT:name:opt1,opt2] → dropdown
 *  - Table cells containing [FORMULA:result=expr] → formula
 *  - Bold-colon label patterns  ("Parameter: ____") → inferred text fields
 *  - Checkbox patterns          "☐ / □ / [ ]"      → boolean field
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocxParserService {

    private static final Pattern FIELD_TAG   = Pattern.compile("\\[FIELD:([\\w.]+)(?::([^\\]]+))?\\]");
    private static final Pattern NUMBER_TAG  = Pattern.compile("\\[NUMBER:([\\w.]+)(?::([^\\]]+))?\\]");
    private static final Pattern DATE_TAG    = Pattern.compile("\\[DATE:([\\w.]+)\\]");
    private static final Pattern SELECT_TAG  = Pattern.compile("\\[SELECT:([\\w.]+):([^\\]]+)\\]");
    private static final Pattern FORMULA_TAG = Pattern.compile("\\[FORMULA:([\\w.]+)=([^\\]]+)\\]");
    private static final Pattern LABEL_BLANK = Pattern.compile("^([A-Za-z /()+%-]{2,40}):\\s*[_\\-]{3,}$");
    private static final Pattern CHECKBOX    = Pattern.compile("[☐□]|\\[\\s*\\]");

    private final ObjectMapper objectMapper;

    public ParsedDocxResult parse(InputStream docxStream) {
        List<Map<String, Object>> fields   = new ArrayList<>();
        List<Map<String, Object>> formulas = new ArrayList<>();
        ArrayNode sections = objectMapper.createArrayNode();

        try (XWPFDocument doc = new XWPFDocument(docxStream)) {

            ObjectNode currentSection = null;
            ArrayNode  sectionFields  = null;
            String     currentHeading = "General";

            for (IBodyElement element : doc.getBodyElements()) {

                if (element instanceof XWPFParagraph para) {
                    String style = para.getStyle();
                    String text  = para.getText().trim();

                    if (text.isEmpty()) continue;

                    if (isHeading(style)) {
                        // Close previous section
                        if (currentSection != null) {
                            sections.add(currentSection);
                        }
                        currentHeading = text;
                        currentSection = objectMapper.createObjectNode();
                        currentSection.put("section", currentHeading);
                        sectionFields = objectMapper.createArrayNode();
                        currentSection.set("fields", sectionFields);
                        continue;
                    }

                    // Inline tags in paragraph text
                    extractTagsFromText(text, fields, formulas, currentHeading);

                    // Label: ____ pattern
                    Matcher lbl = LABEL_BLANK.matcher(text);
                    if (lbl.matches()) {
                        String fieldName = toFieldName(lbl.group(1));
                        if (fieldNotRegistered(fields, fieldName)) {
                            Map<String, Object> f = fieldMap(fieldName, lbl.group(1).trim(), "text", false, null, currentHeading);
                            fields.add(f);
                            if (sectionFields != null) sectionFields.add(objectMapper.valueToTree(f));
                        }
                    }

                    // Checkbox pattern
                    if (CHECKBOX.matcher(text).find()) {
                        String label = CHECKBOX.matcher(text).replaceAll("").trim();
                        if (!label.isEmpty()) {
                            String fieldName = toFieldName(label);
                            if (fieldNotRegistered(fields, fieldName)) {
                                Map<String, Object> f = fieldMap(fieldName, label, "boolean", false, null, currentHeading);
                                fields.add(f);
                                if (sectionFields != null) sectionFields.add(objectMapper.valueToTree(f));
                            }
                        }
                    }

                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        for (XWPFTableCell cell : row.getTableCells()) {
                            String cellText = cell.getText().trim();
                            if (cellText.isEmpty()) continue;

                            List<Map<String, Object>> extracted = extractTagsFromText(cellText, fields, formulas, currentHeading);
                            if (extracted.isEmpty() && sectionFields != null) {
                                // If no tags but has a label:blank pattern
                                Matcher lbl = LABEL_BLANK.matcher(cellText);
                                if (lbl.matches()) {
                                    String fieldName = toFieldName(lbl.group(1));
                                    if (fieldNotRegistered(fields, fieldName)) {
                                        Map<String, Object> f = fieldMap(fieldName, lbl.group(1).trim(), "text", false, null, currentHeading);
                                        fields.add(f);
                                        if (sectionFields != null) sectionFields.add(objectMapper.valueToTree(f));
                                    }
                                }
                            }
                            final ArrayNode sf = sectionFields;
                            extracted.forEach(f -> {
                                if (sf != null) sf.add(objectMapper.valueToTree(f));
                            });
                        }
                    }
                }
            }

            // Close last section
            if (currentSection != null) {
                sections.add(currentSection);
            }

            // If no explicit sections were found, wrap everything in "General"
            if (sections.isEmpty() && !fields.isEmpty()) {
                ObjectNode general = objectMapper.createObjectNode();
                general.put("section", "General");
                ArrayNode gf = objectMapper.createArrayNode();
                fields.forEach(f -> gf.add(objectMapper.valueToTree(f)));
                general.set("fields", gf);
                sections.add(general);
            }

        } catch (Exception e) {
            log.error("DOCX parsing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse DOCX file: " + e.getMessage(), e);
        }

        return new ParsedDocxResult(fields, formulas, sections);
    }

    private List<Map<String, Object>> extractTagsFromText(
            String text,
            List<Map<String, Object>> fields,
            List<Map<String, Object>> formulas,
            String section) {

        List<Map<String, Object>> found = new ArrayList<>();

        Matcher m = FIELD_TAG.matcher(text);
        while (m.find()) {
            String name  = m.group(1);
            String label = m.group(2) != null ? m.group(2) : name;
            if (fieldNotRegistered(fields, name)) {
                Map<String, Object> f = fieldMap(name, label, "text", false, null, section);
                fields.add(f); found.add(f);
            }
        }

        m = NUMBER_TAG.matcher(text);
        while (m.find()) {
            String name  = m.group(1);
            String label = m.group(2) != null ? m.group(2) : name;
            if (fieldNotRegistered(fields, name)) {
                Map<String, Object> f = fieldMap(name, label, "number", false, null, section);
                fields.add(f); found.add(f);
            }
        }

        m = DATE_TAG.matcher(text);
        while (m.find()) {
            String name = m.group(1);
            if (fieldNotRegistered(fields, name)) {
                Map<String, Object> f = fieldMap(name, name, "date", false, null, section);
                fields.add(f); found.add(f);
            }
        }

        m = SELECT_TAG.matcher(text);
        while (m.find()) {
            String name    = m.group(1);
            String options = m.group(2);
            if (fieldNotRegistered(fields, name)) {
                Map<String, Object> f = fieldMap(name, name, "select", false, null, section);
                f.put("options", Arrays.asList(options.split(",")));
                fields.add(f); found.add(f);
            }
        }

        m = FORMULA_TAG.matcher(text);
        while (m.find()) {
            String resultField = m.group(1);
            String expression  = m.group(2);
            Map<String, Object> formula = new LinkedHashMap<>();
            formula.put("fieldName",   resultField);
            formula.put("expression",  expression);
            formulas.add(formula);

            // Also register the result field as a computed number
            if (fieldNotRegistered(fields, resultField)) {
                Map<String, Object> f = fieldMap(resultField, resultField, "number", false, null, section);
                f.put("computed", true);
                fields.add(f); found.add(f);
            }
        }

        return found;
    }

    private boolean isHeading(String style) {
        if (style == null) return false;
        String s = style.toLowerCase();
        return s.contains("heading") || s.equals("title");
    }

    private boolean fieldNotRegistered(List<Map<String, Object>> fields, String name) {
        return fields.stream().noneMatch(f -> name.equals(f.get("name")));
    }

    private Map<String, Object> fieldMap(String name, String label, String type,
                                         boolean required, Object defaultValue, String section) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name",         name);
        m.put("label",        label);
        m.put("type",         type);
        m.put("required",     required);
        m.put("defaultValue", defaultValue);
        m.put("section",      section);
        return m;
    }

    private String toFieldName(String label) {
        return label.trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    public record ParsedDocxResult(
            List<Map<String, Object>> fields,
            List<Map<String, Object>> formulas,
            ArrayNode sections
    ) {}
}
