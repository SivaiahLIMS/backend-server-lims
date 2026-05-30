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
@Tag(name = "QA/QC Module", description = "Deviations, OOS, CAPA management")
public class QaController {

    private final QaService qaService;

    // ── Deviations ────────────────────────────────────────────────────────────

    @GetMapping("/deviations")
    @PreAuthorize("hasAuthority('DEVIATION_VIEW')")
    @Operation(summary = "Get deviations for branch",
               description = "Requires: DEVIATION_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Deviation>> getDeviations(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getDeviations(u.getTenantId(), branchId));
    }

    @PostMapping("/deviations")
    @PreAuthorize("hasAuthority('DEVIATION_CREATE')")
    @Operation(summary = "Create a deviation",
               description = "Requires: DEVIATION_CREATE. branchId must be supplied in the request body.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Deviation> createDeviation(
            @RequestBody CreateDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                qaService.createDeviation(
                        u.getTenantId(),
                        body.getBranchId(),
                        body.getRefEntity(),
                        body.getRefId(),
                        body.getDescription(),
                        body.getSeverity(),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/deviations/{id}/close")
    @PreAuthorize("hasAuthority('DEVIATION_CLOSE')")
    @Operation(summary = "Close a deviation",
               description = "Requires: DEVIATION_CLOSE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Closed"),
        @ApiResponse(responseCode = "404", description = "Deviation not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Deviation> closeDeviation(
            @PathVariable Long id,
            @RequestBody CloseDeviationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeDeviation(id, u.getUser().getId(), body.getRemarks()));
    }

    // ── OOS ───────────────────────────────────────────────────────────────────

    @GetMapping("/oos")
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "Get OOS cases for branch",
               description = "Requires: OOS_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OosCase>> getOos(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getOosCases(u.getTenantId(), branchId));
    }

    @PostMapping("/oos")
    @PreAuthorize("hasAuthority('OOS_CREATE')")
    @Operation(summary = "Create an OOS case",
               description = "Requires: OOS_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OosCase> createOos(
            @RequestBody CreateOosRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                qaService.createOos(
                        u.getTenantId(),
                        body.getBranchId(),
                        body.getSampleId(),
                        body.getTestId(),
                        body.getDescription(),
                        u.getUser().getId()
                )
        );
    }

    // ── CAPA ──────────────────────────────────────────────────────────────────

    @GetMapping("/capa")
    @PreAuthorize("hasAuthority('CAPA_VIEW')")
    @Operation(summary = "Get CAPA list for tenant",
               description = "Requires: CAPA_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<Capa>> getCapa(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getCapas(u.getTenantId()));
    }

    @PostMapping("/capa")
    @PreAuthorize("hasAuthority('CAPA_CREATE')")
    @Operation(summary = "Create a CAPA",
               description = "Requires: CAPA_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Capa> createCapa(
            @RequestBody CreateCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                qaService.createCapa(
                        u.getTenantId(),
                        body.getDeviationId(),
                        body.getActionDesc(),
                        body.getOwnerId(),
                        body.getDueDate()
                )
        );
    }

    @PostMapping("/capa/{id}/close")
    @PreAuthorize("hasAuthority('CAPA_CLOSE')")
    @Operation(summary = "Close a CAPA",
               description = "Requires: CAPA_CLOSE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Closed"),
        @ApiResponse(responseCode = "404", description = "CAPA not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Capa> closeCapa(
            @PathVariable Long id,
            @RequestBody CloseCapaRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeCapa(id, body.getRemarks(), u.getUser().getId()));
    }
}
