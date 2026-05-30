package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.sample.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.SampleService;
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
@RequestMapping("/samples")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sample & Test Module", description = "Sample registration, test assignment, results, COA")
public class SampleController {

    private final SampleService sampleService;

    @PostMapping
    @PreAuthorize("hasAuthority('SAMPLE_REGISTER')")
    @Operation(summary = "Register a sample",
               description = "Requires: SAMPLE_REGISTER. branchId must be in the request body.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registered"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Sample> registerSample(
            @RequestBody RegisterSampleRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.registerSample(
                        u.getTenantId(),
                        body.getBranchId(),
                        body.getSampleNo(),
                        body.getSampleType(),
                        body.getProductName(),
                        body.getBatchNo(),
                        u.getUser().getId()
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get samples for branch",
               description = "Requires: SAMPLE_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<Sample>> getSamples(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSamples(u.getTenantId(), branchId));
    }

    @PostMapping("/{sampleId}/tests")
    @PreAuthorize("hasAuthority('TEST_ASSIGN')")
    @Operation(summary = "Assign a test to a sample",
               description = "Requires: TEST_ASSIGN")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Assigned"),
        @ApiResponse(responseCode = "400", description = "Missing testDefId"),
        @ApiResponse(responseCode = "404", description = "Sample not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<SampleTest> assignTest(
            @PathVariable Long sampleId,
            @RequestBody AssignTestRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.assignTest(sampleId, body.getTestDefId(), body.getAssignedToId())
        );
    }

    @PostMapping("/tests/{sampleTestId}/results")
    @PreAuthorize("hasAuthority('RESULT_ENTER')")
    @Operation(summary = "Enter a test result",
               description = "Requires: RESULT_ENTER")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Result recorded"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "404", description = "Sample test not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TestResult> enterResult(
            @PathVariable Long sampleTestId,
            @RequestBody EnterResultRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.enterResult(
                        sampleTestId,
                        body.getParameterName(),
                        body.getResultValue(),
                        body.getNumericValue(),
                        body.getUnit(),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/results/{resultId}/review")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Review a test result",
               description = "Requires: RESULT_REVIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reviewed"),
        @ApiResponse(responseCode = "404", description = "Result not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<TestResult> reviewResult(
            @PathVariable Long resultId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.reviewResult(resultId, u.getUser().getId()));
    }

    @PostMapping("/{sampleId}/coa/generate")
    @PreAuthorize("hasAuthority('COA_GENERATE')")
    @Operation(summary = "Generate COA for a sample",
               description = "Requires: COA_GENERATE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "COA generated"),
        @ApiResponse(responseCode = "404", description = "Sample not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Coa> generateCoa(
            @PathVariable Long sampleId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.generateCoa(sampleId, u.getTenantId(), branchId, u.getUser().getId())
        );
    }

    @PostMapping("/coa/{coaId}/approve")
    @PreAuthorize("hasAuthority('COA_APPROVE')")
    @Operation(summary = "Approve a COA",
               description = "Requires: COA_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "COA not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Coa> approveCoa(
            @PathVariable Long coaId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.approveCoa(coaId, u.getUser().getId()));
    }
}
