package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.SampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/samples")
@RequiredArgsConstructor
@Tag(name = "Sample & Test Module", description = "Sample registration, test assignment, results, COA")
public class SampleController {

    private final SampleService sampleService;

    @PostMapping
    @PreAuthorize("hasAuthority('SAMPLE_REGISTER')")
    @Operation(summary = "Register a sample")
    public ResponseEntity<Sample> registerSample(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.registerSample(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        (String) body.get("sampleNo"),
                        (String) body.get("sampleType"),
                        (String) body.get("productName"),
                        (String) body.get("batchNo"),
                        u.getUser().getId()
                )
        );
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get samples for branch")
    public ResponseEntity<List<Sample>> getSamples(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSamples(u.getTenantId(), branchId));
    }

    @PostMapping("/{sampleId}/tests")
    @PreAuthorize("hasAuthority('TEST_ASSIGN')")
    @Operation(summary = "Assign a test to a sample")
    public ResponseEntity<SampleTest> assignTest(
            @PathVariable Long sampleId,
            @RequestBody Map<String, Long> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.assignTest(sampleId, body.get("testDefId"), body.get("assignedToId"))
        );
    }

    @PostMapping("/tests/{sampleTestId}/results")
    @PreAuthorize("hasAuthority('RESULT_ENTER')")
    @Operation(summary = "Enter a test result")
    public ResponseEntity<TestResult> enterResult(
            @PathVariable Long sampleTestId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.enterResult(
                        sampleTestId,
                        (String) body.get("parameterName"),
                        (String) body.get("resultValue"),
                        body.containsKey("numericValue") ? new BigDecimal(body.get("numericValue").toString()) : null,
                        (String) body.get("unit"),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/results/{resultId}/review")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Review a test result")
    public ResponseEntity<TestResult> reviewResult(@PathVariable Long resultId,
                                                    @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.reviewResult(resultId, u.getUser().getId()));
    }

    @PostMapping("/{sampleId}/coa/generate")
    @PreAuthorize("hasAuthority('COA_GENERATE')")
    @Operation(summary = "Generate COA for a sample")
    public ResponseEntity<Coa> generateCoa(@PathVariable Long sampleId,
                                           @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
                                            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.generateCoa(sampleId, u.getTenantId(), branchId, u.getUser().getId())
        );
    }

    @PostMapping("/coa/{coaId}/approve")
    @PreAuthorize("hasAuthority('COA_APPROVE')")
    @Operation(summary = "Approve a COA")
    public ResponseEntity<Coa> approveCoa(@PathVariable Long coaId,
                                           @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.approveCoa(coaId, u.getUser().getId()));
    }
}
