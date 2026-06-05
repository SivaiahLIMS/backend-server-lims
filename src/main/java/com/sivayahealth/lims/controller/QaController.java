package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.qa.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.QaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "QA/QC Module", description = "Deviations, OOS/OOT, and CAPA management")
public class QaController {

    private final QaService qaService;

    // ── Deviations ────────────────────────────────────────────────────────────

    @GetMapping("/deviations")
    @PreAuthorize("hasAuthority('DEVIATION_VIEW')")
    @Operation(summary = "List deviations for branch")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "403")})
    public ResponseEntity<List<Deviation>> getDeviations(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getDeviations(u.getTenantId(), branchId));
    }

    @PostMapping("/deviations")
    @PreAuthorize("hasAuthority('DEVIATION_CREATE')")
    @Operation(summary = "Create a deviation")
    @ApiResponses({@ApiResponse(responseCode = "201"), @ApiResponse(responseCode = "403")})
    public ResponseEntity<Deviation> createDeviation(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        body.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.createDeviation(u.getTenantId(), branchId, body, u.getUser().getId()));
    }

    @GetMapping("/deviations/{id}")
    @PreAuthorize("hasAuthority('DEVIATION_VIEW')")
    @Operation(summary = "Get deviation by ID")
    public ResponseEntity<Deviation> getDeviationById(@PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getDeviationById(id));
    }

    @PutMapping("/deviations/{id}")
    @PreAuthorize("hasAuthority('DEVIATION_EDIT')")
    @Operation(summary = "Update a deviation")
    public ResponseEntity<Deviation> updateDeviation(
            @PathVariable Long id,
            @RequestBody UpdateDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateDeviation(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/assign")
    @PreAuthorize("hasAuthority('DEVIATION_EDIT')")
    @Operation(summary = "Assign a deviation to an owner")
    public ResponseEntity<Deviation> assignDeviationOwner(
            @PathVariable Long id,
            @RequestBody AssignDeviationOwnerRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.assignDeviationOwner(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/notes")
    @PreAuthorize("hasAuthority('DEVIATION_EDIT')")
    @Operation(summary = "Add a note to a deviation")
    public ResponseEntity<DeviationNote> addDeviationNote(
            @PathVariable Long id,
            @RequestBody AddDeviationNoteRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.addDeviationNote(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/actions")
    @PreAuthorize("hasAuthority('DEVIATION_EDIT')")
    @Operation(summary = "Add an action item to a deviation")
    public ResponseEntity<DeviationActionItem> addDeviationAction(
            @PathVariable Long id,
            @RequestBody AddDeviationActionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.addDeviationAction(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/actions/{actionId}/status")
    @PreAuthorize("hasAuthority('DEVIATION_EDIT')")
    @Operation(summary = "Update status of a deviation action item")
    public ResponseEntity<DeviationActionItem> updateDeviationActionStatus(
            @PathVariable Long actionId,
            @RequestBody UpdateDeviationActionStatusRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateDeviationActionStatus(actionId, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/status")
    @PreAuthorize("hasAuthority('DEVIATION_EDIT')")
    @Operation(summary = "Update deviation status")
    public ResponseEntity<Deviation> updateDeviationStatus(
            @PathVariable Long id,
            @RequestBody UpdateDeviationStatusRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateDeviationStatus(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/approve")
    @PreAuthorize("hasAuthority('DEVIATION_APPROVE')")
    @Operation(summary = "Approve a deviation")
    public ResponseEntity<Deviation> approveDeviation(
            @PathVariable Long id,
            @RequestBody ApproveDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.approveDeviation(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/reject")
    @PreAuthorize("hasAuthority('DEVIATION_APPROVE')")
    @Operation(summary = "Reject a deviation")
    public ResponseEntity<Deviation> rejectDeviation(
            @PathVariable Long id,
            @RequestBody RejectDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.rejectDeviation(id, body, u.getUser().getId()));
    }

    @PostMapping("/deviations/{id}/close")
    @PreAuthorize("hasAuthority('DEVIATION_CLOSE')")
    @Operation(summary = "Close a deviation")
    public ResponseEntity<Deviation> closeDeviation(
            @PathVariable Long id,
            @RequestBody CloseDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeDeviation(id, body, u.getUser().getId()));
    }

    @GetMapping("/deviations/{id}/audit")
    @PreAuthorize("hasAuthority('DEVIATION_VIEW')")
    @Operation(summary = "Get deviation audit trail")
    public ResponseEntity<List<DeviationAuditTrail>> getDeviationAudit(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getDeviationAuditTrail(id));
    }

    // ── OOS ───────────────────────────────────────────────────────────────────

    @GetMapping("/oos")
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "List OOS cases for branch")
    public ResponseEntity<List<OosCase>> getOos(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getOosCases(u.getTenantId(), branchId));
    }

    @PostMapping("/oos")
    @PreAuthorize("hasAuthority('OOS_CREATE')")
    @Operation(summary = "Create an OOS/OOT case")
    public ResponseEntity<OosCase> createOos(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateOosRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        body.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.createOos(u.getTenantId(), branchId, body, u.getUser().getId()));
    }

    @GetMapping("/oos/{id}")
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "Get OOS case by ID")
    public ResponseEntity<OosCase> getOosById(@PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getOosCaseById(id));
    }

    @PostMapping("/oos/{id}/assign")
    @PreAuthorize("hasAuthority('OOS_EDIT')")
    @Operation(summary = "Assign investigator to OOS case")
    public ResponseEntity<OosCase> assignOosInvestigator(
            @PathVariable Long id,
            @RequestBody AssignOosInvestigatorRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.assignOosInvestigator(id, body, u.getUser().getId()));
    }

    @PostMapping("/oos/{id}/phase1/complete")
    @PreAuthorize("hasAuthority('OOS_EDIT')")
    @Operation(summary = "Complete Phase I investigation")
    public ResponseEntity<OosCase> completePhaseI(
            @PathVariable Long id,
            @RequestBody CompletePhaseIRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.completePhaseI(id, body, u.getUser().getId()));
    }

    @PostMapping("/oos/{id}/phase2/complete")
    @PreAuthorize("hasAuthority('OOS_EDIT')")
    @Operation(summary = "Complete Phase II investigation")
    public ResponseEntity<OosCase> completePhaseII(
            @PathVariable Long id,
            @RequestBody CompletePhaseIIRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.completePhaseII(id, body, u.getUser().getId()));
    }

    @PostMapping("/oos/{id}/close")
    @PreAuthorize("hasAuthority('OOS_CLOSE')")
    @Operation(summary = "Close an OOS case")
    public ResponseEntity<OosCase> closeOosCase(
            @PathVariable Long id,
            @RequestBody CloseOosCaseRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeOosCase(id, body, u.getUser().getId()));
    }

    @PostMapping("/oos/{id}/notes")
    @PreAuthorize("hasAuthority('OOS_EDIT')")
    @Operation(summary = "Add a note to an OOS case")
    public ResponseEntity<OosNote> addOosNote(
            @PathVariable Long id,
            @RequestBody AddOosNoteRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.addOosNote(id, body, u.getUser().getId()));
    }

    @PostMapping("/oos/{id}/actions")
    @PreAuthorize("hasAuthority('OOS_EDIT')")
    @Operation(summary = "Add an action item to an OOS case")
    public ResponseEntity<OosActionItem> addOosAction(
            @PathVariable Long id,
            @RequestBody AddOosActionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.addOosAction(id, body, u.getUser().getId()));
    }

    @PostMapping("/oos/actions/{actionId}/status")
    @PreAuthorize("hasAuthority('OOS_EDIT')")
    @Operation(summary = "Update status of an OOS action item")
    public ResponseEntity<OosActionItem> updateOosActionStatus(
            @PathVariable Long actionId,
            @RequestBody UpdateOosActionStatusRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateOosActionStatus(actionId, body, u.getUser().getId()));
    }

    @GetMapping("/oos/{id}/audit")
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "Get OOS case audit trail")
    public ResponseEntity<List<OosAuditTrail>> getOosAudit(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getOosAuditTrail(id));
    }

    // ── CAPA ──────────────────────────────────────────────────────────────────

    @GetMapping("/capa")
    @PreAuthorize("hasAuthority('CAPA_VIEW')")
    @Operation(summary = "List CAPAs for tenant")
    public ResponseEntity<List<Capa>> getCapa(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getCapas(u.getTenantId()));
    }

    @PostMapping("/capa")
    @PreAuthorize("hasAuthority('CAPA_CREATE')")
    @Operation(summary = "Create a CAPA")
    public ResponseEntity<Capa> createCapa(
            @RequestBody CreateCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.createCapa(u.getTenantId(), body, u.getUser().getId()));
    }

    @GetMapping("/capa/{id}")
    @PreAuthorize("hasAuthority('CAPA_VIEW')")
    @Operation(summary = "Get CAPA by ID")
    public ResponseEntity<Capa> getCapaById(@PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getCapaById(id));
    }

    @PutMapping("/capa/{id}")
    @PreAuthorize("hasAuthority('CAPA_EDIT')")
    @Operation(summary = "Update a CAPA")
    public ResponseEntity<Capa> updateCapa(
            @PathVariable Long id,
            @RequestBody UpdateCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateCapa(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/assign")
    @PreAuthorize("hasAuthority('CAPA_EDIT')")
    @Operation(summary = "Assign CAPA owner")
    public ResponseEntity<Capa> assignCapaOwner(
            @PathVariable Long id,
            @RequestBody AssignCapaOwnerRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.assignCapaOwner(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/notes")
    @PreAuthorize("hasAuthority('CAPA_EDIT')")
    @Operation(summary = "Add a note to a CAPA")
    public ResponseEntity<CapaNote> addCapaNote(
            @PathVariable Long id,
            @RequestBody AddCapaNoteRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.addCapaNote(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/actions")
    @PreAuthorize("hasAuthority('CAPA_EDIT')")
    @Operation(summary = "Add an action item to a CAPA")
    public ResponseEntity<CapaActionItem> addCapaAction(
            @PathVariable Long id,
            @RequestBody AddCapaActionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(qaService.addCapaAction(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/actions/{actionId}/status")
    @PreAuthorize("hasAuthority('CAPA_EDIT')")
    @Operation(summary = "Update status of a CAPA action item")
    public ResponseEntity<CapaActionItem> updateCapaActionStatus(
            @PathVariable Long actionId,
            @RequestBody UpdateCapaActionStatusRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateCapaActionStatus(actionId, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/status")
    @PreAuthorize("hasAuthority('CAPA_EDIT')")
    @Operation(summary = "Update CAPA status")
    public ResponseEntity<Capa> updateCapaStatus(
            @PathVariable Long id,
            @RequestBody UpdateCapaStatusRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.updateCapaStatus(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/approve")
    @PreAuthorize("hasAuthority('CAPA_APPROVE')")
    @Operation(summary = "Approve a CAPA")
    public ResponseEntity<Capa> approveCapa(
            @PathVariable Long id,
            @RequestBody ApproveCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.approveCapa(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/reject")
    @PreAuthorize("hasAuthority('CAPA_APPROVE')")
    @Operation(summary = "Reject a CAPA")
    public ResponseEntity<Capa> rejectCapa(
            @PathVariable Long id,
            @RequestBody RejectCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.rejectCapa(id, body, u.getUser().getId()));
    }

    @PostMapping("/capa/{id}/close")
    @PreAuthorize("hasAuthority('CAPA_CLOSE')")
    @Operation(summary = "Close a CAPA")
    public ResponseEntity<Capa> closeCapa(
            @PathVariable Long id,
            @RequestBody CloseCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeCapa(id, body, u.getUser().getId()));
    }
}
