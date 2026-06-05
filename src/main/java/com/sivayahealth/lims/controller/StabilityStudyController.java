package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.StabilityStudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/stability")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Stability Study", description = "Stability study lifecycle: create, manage timepoints and results")
public class StabilityStudyController {

    private final StabilityStudyService stabilityStudyService;

    // ── Study CRUD ───────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('STABILITY_VIEW')")
    @Operation(summary = "List stability studies for branch")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<StabilityStudy>> list(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<StabilityStudy> studies = status != null
                ? stabilityStudyService.getStudiesByStatus(u.getTenantId(), branchId, status)
                : stabilityStudyService.getStudies(u.getTenantId(), branchId);
        return ResponseEntity.ok(studies);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STABILITY_VIEW')")
    @Operation(summary = "Get stability study by ID")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK"), @ApiResponse(responseCode = "404", description = "Not found")})
    public ResponseEntity<StabilityStudy> getById(@PathVariable Long id) {
        return ResponseEntity.ok(stabilityStudyService.getStudyById(id));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAuthority('STABILITY_VIEW')")
    @Operation(summary = "Get stability studies for a product")
    public ResponseEntity<List<StabilityStudy>> byProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stabilityStudyService.getStudiesByProduct(productId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STABILITY_CREATE')")
    @Operation(summary = "Create a new stability study")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Created")})
    public ResponseEntity<StabilityStudy> create(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateStudyRequest req,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.createStudy(
                u.getTenantId(), branchId, req.getProductId(), req.getTitle(),
                req.getStudyType(), req.getProtocol(), req.getStorageCondition(),
                req.getStartDate(), u.getUser().getId()));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('STABILITY_MANAGE')")
    @Operation(summary = "Activate a DRAFT stability study")
    public ResponseEntity<StabilityStudy> activate(@PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.activateStudy(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('STABILITY_MANAGE')")
    @Operation(summary = "Complete an ACTIVE stability study")
    public ResponseEntity<StabilityStudy> complete(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.completeStudy(id, endDate, u.getUser().getId()));
    }

    @PostMapping("/{id}/discontinue")
    @PreAuthorize("hasAuthority('STABILITY_MANAGE')")
    @Operation(summary = "Discontinue a stability study")
    public ResponseEntity<StabilityStudy> discontinue(@PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.discontinueStudy(id, u.getUser().getId()));
    }

    // ── Timepoints ───────────────────────────────────────────────────────────────

    @GetMapping("/{id}/timepoints")
    @PreAuthorize("hasAuthority('STABILITY_VIEW')")
    @Operation(summary = "List timepoints for a study")
    public ResponseEntity<List<StabilityStudyTimepoint>> timepoints(@PathVariable Long id) {
        return ResponseEntity.ok(stabilityStudyService.getTimepoints(id));
    }

    @PostMapping("/{id}/timepoints")
    @PreAuthorize("hasAuthority('STABILITY_MANAGE')")
    @Operation(summary = "Add a timepoint to a stability study")
    public ResponseEntity<StabilityStudyTimepoint> addTimepoint(
            @PathVariable Long id,
            @RequestBody AddTimepointRequest req,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.addTimepoint(
                id, req.getTimepoint(), req.getScheduledDate(), u.getUser().getId()));
    }

    @PostMapping("/timepoints/{timepointId}/complete")
    @PreAuthorize("hasAuthority('STABILITY_MANAGE')")
    @Operation(summary = "Mark a timepoint as completed")
    public ResponseEntity<StabilityStudyTimepoint> completeTimepoint(
            @PathVariable Long timepointId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.completeTimepoint(timepointId, u.getUser().getId()));
    }

    // ── Results ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAuthority('STABILITY_VIEW')")
    @Operation(summary = "Get all results for a study")
    public ResponseEntity<List<StabilityStudyResult>> allResults(@PathVariable Long id) {
        return ResponseEntity.ok(stabilityStudyService.getAllResults(id));
    }

    @GetMapping("/timepoints/{timepointId}/results")
    @PreAuthorize("hasAuthority('STABILITY_VIEW')")
    @Operation(summary = "Get results for a specific timepoint")
    public ResponseEntity<List<StabilityStudyResult>> resultsByTimepoint(@PathVariable Long timepointId) {
        return ResponseEntity.ok(stabilityStudyService.getResults(timepointId));
    }

    @PostMapping("/timepoints/{timepointId}/results")
    @PreAuthorize("hasAuthority('STABILITY_MANAGE')")
    @Operation(summary = "Record a test result for a timepoint")
    public ResponseEntity<StabilityStudyResult> recordResult(
            @PathVariable Long timepointId,
            @RequestBody RecordResultRequest req,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(stabilityStudyService.recordResult(
                timepointId, req.getParameter(), req.getSpecification(),
                req.getResult(), req.getPassFail(), req.getRemarks(), u.getUser().getId()));
    }

    // ── Inner DTOs ───────────────────────────────────────────────────────────────

    @Data static class CreateStudyRequest {
        private Long productId;
        private String title;
        private String studyType;
        private String protocol;
        private String storageCondition;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
    }

    @Data static class AddTimepointRequest {
        private String timepoint;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate scheduledDate;
    }

    @Data static class RecordResultRequest {
        private String parameter;
        private String specification;
        private String result;
        private String passFail;
        private String remarks;
    }
}
