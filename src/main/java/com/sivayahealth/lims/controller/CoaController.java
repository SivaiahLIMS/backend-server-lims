package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.Coa;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.CoaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coa")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "COA", description = "Certificate of Analysis lifecycle: DRAFT → PENDING_APPROVAL → APPROVED → ISSUED")
public class CoaController {

    private final CoaService coaService;

    @GetMapping
    @PreAuthorize("hasAuthority('COA_VIEW')")
    @Operation(summary = "List all COAs for tenant")
    public ResponseEntity<List<Coa>> getCoas(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.getCoasByTenant(u.getTenantId()));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('COA_VIEW')")
    @Operation(summary = "List COAs by status (DRAFT, PENDING_APPROVAL, APPROVED, ISSUED, REJECTED)")
    public ResponseEntity<List<Coa>> getCoasByStatus(
            @PathVariable String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.getCoasByStatus(u.getTenantId(), status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('COA_VIEW')")
    @Operation(summary = "Get COA by ID")
    public ResponseEntity<Coa> getCoaById(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.getCoaById(id));
    }

    @GetMapping("/sample/{sampleId}")
    @PreAuthorize("hasAuthority('COA_VIEW')")
    @Operation(summary = "Get COA for a specific sample")
    public ResponseEntity<Coa> getCoaBySample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.getCoaBySample(sampleId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('COA_CREATE')")
    @Operation(summary = "Generate a new COA for a sample")
    public ResponseEntity<Coa> generateCoa(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody GenerateCoaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                coaService.generateCoa(
                        u.getTenantId(), branchId,
                        body.getSampleId(), body.getProductId(),
                        body.getTestResultsJson(), u.getUser().getId()));
    }

    @PutMapping("/{id}/test-results")
    @PreAuthorize("hasAuthority('COA_EDIT')")
    @Operation(summary = "Update test results on a DRAFT COA")
    public ResponseEntity<Coa> updateTestResults(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                coaService.updateTestResults(id, body.get("testResultsJson"), u.getUser().getId()));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('COA_SUBMIT')")
    @Operation(summary = "Submit COA for approval")
    public ResponseEntity<Coa> submitForApproval(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.submitCoaForApproval(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('COA_APPROVE')")
    @Operation(summary = "Approve a COA")
    public ResponseEntity<Coa> approveCoa(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.approveCoa(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('COA_APPROVE')")
    @Operation(summary = "Reject a COA")
    public ResponseEntity<Coa> rejectCoa(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.rejectCoa(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAuthority('COA_ISSUE')")
    @Operation(summary = "Issue an approved COA to the customer")
    public ResponseEntity<Coa> issueCoa(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(coaService.issueCoa(id, u.getUser().getId()));
    }

    @Data
    static class GenerateCoaRequest {
        private Long sampleId;
        private Long productId;
        private String testResultsJson;
    }
}
