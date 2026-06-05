package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.validation.UpsertValidationRuleRequest;
import com.sivayahealth.lims.dto.validation.ValidateFieldRequest;
import com.sivayahealth.lims.dto.validation.ValidateFieldResponse;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WorksheetValidationService {

    private static final Set<String> VALID_RULE_TYPES =
            Set.of("RANGE", "MIN_ONLY", "MAX_ONLY", "TARGET_VARIANCE", "FORMULA_BASED");

    private final WorksheetFieldValidationRuleRepository ruleRepository;
    private final WorksheetValidationEventRepository     eventRepository;
    private final DocumentFieldSlotRepository            slotRepository;
    private final WorksheetMasterRepository              worksheetRepository;
    private final AppUserRepository                      appUserRepository;

    // ── Rule management ───────────────────────────────────────────────────────

    public WorksheetFieldValidationRule getRuleForSlot(Long slotId) {
        return ruleRepository.findBySlot_SlotIdAndActiveTrue(slotId).orElse(null);
    }

    @Transactional
    public WorksheetFieldValidationRule upsertRule(Long slotId,
                                                    UpsertValidationRuleRequest req,
                                                    Long userId) {
        DocumentFieldSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> LimsException.notFound("Field slot not found: " + slotId));

        String ruleType = req.getRuleType() != null ? req.getRuleType().toUpperCase() : "RANGE";
        if (!VALID_RULE_TYPES.contains(ruleType)) {
            throw LimsException.badRequest(
                    "Invalid ruleType '" + ruleType + "'. Must be one of: " + VALID_RULE_TYPES);
        }

        validateLimits(req, ruleType);

        ruleRepository.findBySlot_SlotIdAndActiveTrue(slotId).ifPresent(existing -> {
            existing.setActive(false);
            existing.setUpdatedAt(LocalDateTime.now());
            ruleRepository.save(existing);
        });

        AppUser user = userId != null ? appUserRepository.findById(userId).orElse(null) : null;

        WorksheetFieldValidationRule rule = WorksheetFieldValidationRule.builder()
                .slot(slot)
                .fieldType(req.getFieldType() != null ? req.getFieldType() : "NUMBER")
                .ruleType(ruleType)
                .unit(req.getUnit())
                .oosLowerLimit(req.getOosLowerLimit())
                .oosUpperLimit(req.getOosUpperLimit())
                .ootLowerLimit(req.getOotLowerLimit())
                .ootUpperLimit(req.getOotUpperLimit())
                .requireCommentOnOos(req.isRequireCommentOnOos())
                .requireCommentOnOot(req.isRequireCommentOnOot())
                .requireReviewOnOot(req.isRequireReviewOnOot())
                .requireInvestigationOnOos(req.isRequireInvestigationOnOos())
                .requireCapaOnOos(req.isRequireCapaOnOos())
                .active(req.isActive())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        return ruleRepository.save(rule);
    }

    // ── Core validation logic ─────────────────────────────────────────────────

    public ValidateFieldResponse validate(Long slotId, BigDecimal value) {
        Optional<WorksheetFieldValidationRule> ruleOpt =
                ruleRepository.findBySlot_SlotIdAndActiveTrue(slotId);

        if (ruleOpt.isEmpty()) {
            return ValidateFieldResponse.builder()
                    .status("NO_RULE").oos(false).oot(false)
                    .severity("NONE")
                    .message("No active validation rule configured for this field.")
                    .requiresComment(false).requiresReview(false)
                    .requiresInvestigation(false).requiresCapa(false)
                    .build();
        }

        WorksheetFieldValidationRule rule = ruleOpt.get();

        if (value == null) {
            return ValidateFieldResponse.builder()
                    .status("PASS").oos(false).oot(false)
                    .severity("LOW")
                    .message("No value provided — skipping validation.")
                    .requiresComment(false).requiresReview(false)
                    .requiresInvestigation(false).requiresCapa(false)
                    .build();
        }

        boolean oosViolation = checkOos(rule, value);
        if (oosViolation) {
            return ValidateFieldResponse.builder()
                    .status("OOS").oos(true).oot(false)
                    .severity("HIGH")
                    .message(String.format("Value %s%s is Out of Specification %s",
                            value.toPlainString(), unitSuffix(rule.getUnit()),
                            formatOosLimits(rule)))
                    .requiresComment(rule.isRequireCommentOnOos())
                    .requiresReview(false)
                    .requiresInvestigation(rule.isRequireInvestigationOnOos())
                    .requiresCapa(rule.isRequireCapaOnOos())
                    .build();
        }

        boolean ootViolation = checkOot(rule, value);
        if (ootViolation) {
            return ValidateFieldResponse.builder()
                    .status("OOT").oos(false).oot(true)
                    .severity("MEDIUM")
                    .message(String.format("Value %s%s is Out of Trend %s",
                            value.toPlainString(), unitSuffix(rule.getUnit()),
                            formatOotLimits(rule)))
                    .requiresComment(rule.isRequireCommentOnOot())
                    .requiresReview(rule.isRequireReviewOnOot())
                    .requiresInvestigation(false)
                    .requiresCapa(false)
                    .build();
        }

        return ValidateFieldResponse.builder()
                .status("PASS").oos(false).oot(false)
                .severity("LOW")
                .message(String.format("Value %s%s is within specification.",
                        value.toPlainString(), unitSuffix(rule.getUnit())))
                .requiresComment(false).requiresReview(false)
                .requiresInvestigation(false).requiresCapa(false)
                .build();
    }

    // ── On-blur field validation (records event) ──────────────────────────────

    @Transactional
    public ValidateFieldResponse validateForWorksheet(Long worksheetId, Long slotId,
                                                       ValidateFieldRequest req, Long userId) {
        WorksheetMaster worksheet = worksheetRepository.findById(worksheetId)
                .orElseThrow(() -> LimsException.notFound("Worksheet not found: " + worksheetId));

        DocumentFieldSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> LimsException.notFound("Field slot not found: " + slotId));

        if (worksheet.getDocumentVersion() != null
                && !slot.getDocumentVersion().getId()
                        .equals(worksheet.getDocumentVersion().getId())) {
            throw LimsException.badRequest(
                    "Slot " + slotId + " does not belong to the document version of worksheet " + worksheetId);
        }

        ValidateFieldResponse result = validate(slotId, req.getValue());
        recordEvent(worksheetId, slotId, req.getValue(), req.getUnit(), result, "FIELD_BLUR", userId);
        return result;
    }

    // ── Computed-result validation (called after formula compute) ─────────────

    @Transactional
    public ValidateFieldResponse validateComputedResult(Long worksheetId, Long slotId,
                                                         BigDecimal value, String unit, Long userId) {
        ValidateFieldResponse result = validate(slotId, value);
        recordEvent(worksheetId, slotId, value, unit, result, "COMPUTED_RESULT", userId);
        return result;
    }

    // ── Validation event history ──────────────────────────────────────────────

    public List<WorksheetValidationEvent> getValidationEvents(Long worksheetId) {
        return eventRepository.findByWorksheetIdOrderByValidatedAtDesc(worksheetId);
    }

    public List<WorksheetValidationEvent> getValidationEventsForField(Long worksheetId, Long slotId) {
        return eventRepository.findByWorksheetIdAndSlotIdOrderByValidatedAtDesc(worksheetId, slotId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean checkOos(WorksheetFieldValidationRule rule, BigDecimal value) {
        return switch (rule.getRuleType() != null ? rule.getRuleType() : "RANGE") {
            case "MIN_ONLY" ->
                    rule.getOosLowerLimit() != null && value.compareTo(rule.getOosLowerLimit()) < 0;
            case "MAX_ONLY" ->
                    rule.getOosUpperLimit() != null && value.compareTo(rule.getOosUpperLimit()) > 0;
            default -> {
                boolean low  = rule.getOosLowerLimit() != null && value.compareTo(rule.getOosLowerLimit()) < 0;
                boolean high = rule.getOosUpperLimit() != null && value.compareTo(rule.getOosUpperLimit()) > 0;
                yield low || high;
            }
        };
    }

    private boolean checkOot(WorksheetFieldValidationRule rule, BigDecimal value) {
        return switch (rule.getRuleType() != null ? rule.getRuleType() : "RANGE") {
            case "MIN_ONLY" ->
                    rule.getOotLowerLimit() != null && value.compareTo(rule.getOotLowerLimit()) < 0;
            case "MAX_ONLY" ->
                    rule.getOotUpperLimit() != null && value.compareTo(rule.getOotUpperLimit()) > 0;
            default -> {
                boolean low  = rule.getOotLowerLimit() != null && value.compareTo(rule.getOotLowerLimit()) < 0;
                boolean high = rule.getOotUpperLimit() != null && value.compareTo(rule.getOotUpperLimit()) > 0;
                yield low || high;
            }
        };
    }

    private void recordEvent(Long worksheetId, Long slotId, BigDecimal value, String unit,
                              ValidateFieldResponse result, String source, Long userId) {
        WorksheetValidationEvent event = WorksheetValidationEvent.builder()
                .worksheetId(worksheetId)
                .slotId(slotId)
                .value(value)
                .unit(unit)
                .status(result.getStatus())
                .severity(result.getSeverity())
                .message(result.getMessage())
                .requiresComment(result.isRequiresComment())
                .requiresReview(result.isRequiresReview())
                .requiresInvestigation(result.isRequiresInvestigation())
                .requiresCapa(result.isRequiresCapa())
                .source(source)
                .validatedBy(userId)
                .validatedAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);
    }

    private void validateLimits(UpsertValidationRuleRequest req, String ruleType) {
        if ("TARGET_VARIANCE".equals(ruleType) || "FORMULA_BASED".equals(ruleType)) return;
        if (req.getOotLowerLimit() != null && req.getOosLowerLimit() != null
                && req.getOotLowerLimit().compareTo(req.getOosLowerLimit()) < 0) {
            throw LimsException.badRequest("OOT lower limit must be >= OOS lower limit.");
        }
        if (req.getOotUpperLimit() != null && req.getOosUpperLimit() != null
                && req.getOotUpperLimit().compareTo(req.getOosUpperLimit()) > 0) {
            throw LimsException.badRequest("OOT upper limit must be <= OOS upper limit.");
        }
    }

    private String formatOosLimits(WorksheetFieldValidationRule rule) {
        return switch (rule.getRuleType() != null ? rule.getRuleType() : "RANGE") {
            case "MIN_ONLY" -> "(min: " + fmt(rule.getOosLowerLimit(), rule.getUnit()) + ")";
            case "MAX_ONLY" -> "(max: " + fmt(rule.getOosUpperLimit(), rule.getUnit()) + ")";
            default -> "(" + fmt(rule.getOosLowerLimit(), rule.getUnit())
                     + " - " + fmt(rule.getOosUpperLimit(), rule.getUnit()) + ")";
        };
    }

    private String formatOotLimits(WorksheetFieldValidationRule rule) {
        return switch (rule.getRuleType() != null ? rule.getRuleType() : "RANGE") {
            case "MIN_ONLY" -> "(min: " + fmt(rule.getOotLowerLimit(), rule.getUnit()) + ")";
            case "MAX_ONLY" -> "(max: " + fmt(rule.getOotUpperLimit(), rule.getUnit()) + ")";
            default -> "(" + fmt(rule.getOotLowerLimit(), rule.getUnit())
                     + " - " + fmt(rule.getOotUpperLimit(), rule.getUnit()) + ")";
        };
    }

    private String fmt(BigDecimal v, String unit) {
        if (v == null) return "-";
        return v.toPlainString() + (unit != null ? unit : "");
    }

    private String unitSuffix(String unit) {
        return unit != null && !unit.isBlank() ? unit : "";
    }
}
