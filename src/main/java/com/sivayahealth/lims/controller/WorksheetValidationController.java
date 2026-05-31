package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.validation.UpsertValidationRuleRequest;
import com.sivayahealth.lims.dto.validation.ValidateFieldRequest;
import com.sivayahealth.lims.dto.validation.ValidateFieldResponse;
import com.sivayahealth.lims.entity.WorksheetFieldValidationRule;
import com.sivayahealth.lims.entity.WorksheetValidationEvent;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.WorksheetValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Worksheet Field Validation Rules",
     description = "OOS/OOT validation rules configured at document template field level. " +
                   "Rules apply automatically to every worksheet execution that uses the template.")
public class WorksheetValidationController {

    private final WorksheetValidationService validationService;

    // ── Template-level rule management ────────────────────────────────────────

    @GetMapping("/worksheet-templates/{templateId}/fields/{fieldId}/validation-rule")
    @PreAuthorize("hasAuthority('VALIDATION_RULE_VIEW')")
    @Operation(
        summary = "Get the active OOS/OOT validation rule for a template field",
        description = "Requires: VALIDATION_RULE_VIEW. " +
                      "templateId is the document_version_id; fieldId is the slot_id. " +
                      "Returns 404 if no active rule is configured."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active rule returned"),
        @ApiResponse(responseCode = "404", description = "No active rule for this field"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<WorksheetFieldValidationRule> getRuleForTemplateField(
            @PathVariable Long templateId,
            @PathVariable Long fieldId) {
        WorksheetFieldValidationRule rule = validationService.getRuleForSlot(fieldId);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }

    @PutMapping("/worksheet-templates/{templateId}/fields/{fieldId}/validation-rule")
    @PreAuthorize("hasAuthority('VALIDATION_RULE_MANAGE')")
    @Operation(
        summary = "Configure the OOS/OOT rule for a template field",
        description = "Requires: VALIDATION_RULE_MANAGE. " +
                      "Any existing active rule for the field is deactivated and replaced. " +
                      "OOT limits must be inside OOS limits when both are specified. " +
                      "fieldId is the slot_id from document_field_slot."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rule saved"),
        @ApiResponse(responseCode = "400", description = "Invalid limits (e.g. OOT outside OOS)"),
        @ApiResponse(responseCode = "404", description = "Field slot not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<WorksheetFieldValidationRule> upsertRuleForTemplateField(
            @PathVariable Long templateId,
            @PathVariable Long fieldId,
            @RequestBody UpsertValidationRuleRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                validationService.upsertRule(fieldId, body, u.getUser().getId())
        );
    }

    // ── Worksheet-level rule view ─────────────────────────────────────────────

    @GetMapping("/worksheets/{worksheetId}/fields/{fieldId}/validation-rule")
    @PreAuthorize("hasAuthority('VALIDATION_RULE_VIEW')")
    @Operation(
        summary = "Get the active OOS/OOT validation rule for a field within a specific worksheet",
        description = "Requires: VALIDATION_RULE_VIEW. " +
                      "The rule is template-level (same rule regardless of which worksheet execution). " +
                      "fieldId is the slot_id."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Active rule returned"),
        @ApiResponse(responseCode = "404", description = "No active rule or field not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<WorksheetFieldValidationRule> getRuleForWorksheetField(
            @PathVariable Long worksheetId,
            @PathVariable Long fieldId) {
        WorksheetFieldValidationRule rule = validationService.getRuleForSlot(fieldId);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }

    // ── On-the-fly validation (textbox on-blur) ───────────────────────────────

    @PostMapping("/worksheets/{worksheetId}/fields/{fieldId}/validate")
    @PreAuthorize("hasAuthority('WORKSHEET_FIELD_FILL')")
    @Operation(
        summary = "Validate a field value against the OOS/OOT rule (call on textbox blur)",
        description = "Requires: WORKSHEET_FIELD_FILL. " +
                      "fieldId is the slot_id. " +
                      "Returns status PASS | OOT | OOS | NO_RULE. " +
                      "OOS → severity HIGH; OOT → MEDIUM; PASS → LOW; NO_RULE → NONE. " +
                      "When requiresComment=true the UI must show a mandatory comment field before saving. " +
                      "Each validation call is recorded in the worksheet validation event log."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Validation result"),
        @ApiResponse(responseCode = "400", description = "Slot does not belong to this worksheet's template"),
        @ApiResponse(responseCode = "404", description = "Worksheet or field slot not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ValidateFieldResponse> validateField(
            @PathVariable Long worksheetId,
            @PathVariable Long fieldId,
            @RequestBody ValidateFieldRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                validationService.validateForWorksheet(worksheetId, fieldId, body, u.getUser().getId())
        );
    }

    // ── Validation event log ──────────────────────────────────────────────────

    @GetMapping("/worksheets/{worksheetId}/validation-events")
    @PreAuthorize("hasAuthority('VALIDATION_RULE_VIEW')")
    @Operation(
        summary = "Get all OOS/OOT validation events for a worksheet",
        description = "Requires: VALIDATION_RULE_VIEW. " +
                      "Returns all validation calls made on any field of this worksheet, " +
                      "ordered most-recent first. Useful for review, audit trail, and QA sign-off."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Event list returned"),
        @ApiResponse(responseCode = "404", description = "Worksheet not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<WorksheetValidationEvent>> getValidationEvents(
            @PathVariable Long worksheetId) {
        return ResponseEntity.ok(validationService.getValidationEvents(worksheetId));
    }
}
