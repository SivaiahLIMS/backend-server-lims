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

@Service
@RequiredArgsConstructor
public class WorksheetValidationService {

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

        validateLimits(req);

        ruleRepository.findBySlot_SlotIdAndActiveTrue(slotId).ifPresent(existing -> {
            existing.setActive(false);
            existing.setUpdatedAt(LocalDateTime.now());
            ruleRepository.save(existing);
        });

        AppUser user = userId != null ? appUserRepository.findById(userId).orElse(null) : null;

        WorksheetFieldValidationRule rule = WorksheetFieldValidationRule.builder()
                .slot(slot)
                .fieldType(req.getFieldType() != null ? req.getFieldType() : "NUMBER")
                .unit(req.getUnit())
                .oosLowerLimit(req.getOosLowerLimit())
                .oosUpperLimit(req.getOosUpperLimit())
                .ootLowerLimit(req.getOotLowerLimit())
                .ootUpperLimit(req.getOotUpperLimit())
                .requireCommentOnOos(req.isRequireCommentOnOos())
                .requireCommentOnOot(req.isRequireCommentOnOot())
                .active(req.isActive())
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();

        return ruleRepository.save(rule);
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public ValidateFieldResponse validate(Long slotId, ValidateFieldRequest req) {
        Optional<WorksheetFieldValidationRule> ruleOpt =
                ruleRepository.findBySlot_SlotIdAndActiveTrue(slotId);

        if (ruleOpt.isEmpty()) {
            return ValidateFieldResponse.builder()
                    .status("NO_RULE").oos(false).oot(false)
                    .severity("NONE")
                    .message("No active validation rule configured for this field.")
                    .requiresComment(false)
                    .build();
        }

        WorksheetFieldValidationRule rule = ruleOpt.get();
        BigDecimal value = req.getValue();

        if (value == null) {
            return ValidateFieldResponse.builder()
                    .status("PASS").oos(false).oot(false)
                    .severity("LOW")
                    .message("No value provided — skipping validation.")
                    .requiresComment(false)
                    .build();
        }

        boolean oosLow  = rule.getOosLowerLimit() != null && value.compareTo(rule.getOosLowerLimit()) < 0;
        boolean oosHigh = rule.getOosUpperLimit() != null && value.compareTo(rule.getOosUpperLimit()) > 0;
        if (oosLow || oosHigh) {
            return ValidateFieldResponse.builder()
                    .status("OOS").oos(true).oot(false)
                    .severity("HIGH")
                    .message(String.format("Value %s%s is outside OOS limit %s",
                            value.toPlainString(), unitSuffix(rule.getUnit()),
                            formatLimits(rule.getOosLowerLimit(), rule.getOosUpperLimit(), rule.getUnit())))
                    .requiresComment(rule.isRequireCommentOnOos())
                    .build();
        }

        boolean ootLow  = rule.getOotLowerLimit() != null && value.compareTo(rule.getOotLowerLimit()) < 0;
        boolean ootHigh = rule.getOotUpperLimit() != null && value.compareTo(rule.getOotUpperLimit()) > 0;
        if (ootLow || ootHigh) {
            return ValidateFieldResponse.builder()
                    .status("OOT").oos(false).oot(true)
                    .severity("MEDIUM")
                    .message(String.format("Value %s%s is outside OOT trend limit %s",
                            value.toPlainString(), unitSuffix(rule.getUnit()),
                            formatLimits(rule.getOotLowerLimit(), rule.getOotUpperLimit(), rule.getUnit())))
                    .requiresComment(rule.isRequireCommentOnOot())
                    .build();
        }

        return ValidateFieldResponse.builder()
                .status("PASS").oos(false).oot(false)
                .severity("LOW")
                .message(String.format("Value %s%s is within specification.",
                        value.toPlainString(), unitSuffix(rule.getUnit())))
                .requiresComment(false)
                .build();
    }

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
            throw LimsException.notFound(
                    "Slot " + slotId + " does not belong to the document version of worksheet " + worksheetId);
        }

        ValidateFieldResponse result = validate(slotId, req);

        WorksheetValidationEvent event = WorksheetValidationEvent.builder()
                .worksheetId(worksheetId)
                .slotId(slotId)
                .value(req.getValue())
                .unit(req.getUnit())
                .status(result.getStatus())
                .severity(result.getSeverity())
                .message(result.getMessage())
                .requiresComment(result.isRequiresComment())
                .validatedBy(userId)
                .validatedAt(LocalDateTime.now())
                .build();
        eventRepository.save(event);

        return result;
    }

    public List<WorksheetValidationEvent> getValidationEvents(Long worksheetId) {
        return eventRepository.findByWorksheetIdOrderByValidatedAtDesc(worksheetId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void validateLimits(UpsertValidationRuleRequest req) {
        if (req.getOotLowerLimit() != null && req.getOosLowerLimit() != null
                && req.getOotLowerLimit().compareTo(req.getOosLowerLimit()) < 0) {
            throw LimsException.notFound("OOT lower limit must be >= OOS lower limit.");
        }
        if (req.getOotUpperLimit() != null && req.getOosUpperLimit() != null
                && req.getOotUpperLimit().compareTo(req.getOosUpperLimit()) > 0) {
            throw LimsException.notFound("OOT upper limit must be <= OOS upper limit.");
        }
    }

    private String formatLimits(BigDecimal lower, BigDecimal upper, String unit) {
        String u = unit != null ? unit : "";
        String lo = lower != null ? lower.toPlainString() + u : "-\u221e";
        String hi = upper != null ? upper.toPlainString() + u : "+\u221e";
        return lo + " - " + hi;
    }

    private String unitSuffix(String unit) {
        return unit != null && !unit.isBlank() ? unit : "";
    }
}
