package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.sample.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.SampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/samples")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Sample & Test Module", description = "Sample registration, test assignment, results, COA")
public class SampleController {

    private final SampleService sampleService;

    // ---- Samples ----

    @PostMapping
    @PreAuthorize("hasAuthority('SAMPLE_REGISTER')")
    @Operation(summary = "Register a new sample")
    public ResponseEntity<Sample> registerSample(
            @RequestBody RegisterSampleRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.registerSample(u.getTenantId(), body, u.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "List samples for a branch")
    public ResponseEntity<List<Sample>> getSamples(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSamples(u.getTenantId(), branchId));
    }

    @GetMapping("/{sampleId}")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get sample details")
    public ResponseEntity<Sample> getSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSample(sampleId));
    }

    @PostMapping("/{sampleId}/receive")
    @PreAuthorize("hasAuthority('SAMPLE_RECEIVE')")
    @Operation(summary = "Mark sample as received")
    public ResponseEntity<Sample> receiveSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.receiveSample(sampleId, u.getUser().getId()));
    }

    @PostMapping("/{sampleId}/approve")
    @PreAuthorize("hasAuthority('SAMPLE_APPROVE')")
    @Operation(summary = "Approve a sample")
    public ResponseEntity<Sample> approveSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.approveSample(sampleId, u.getUser().getId()));
    }

    @PostMapping("/{sampleId}/reject")
    @PreAuthorize("hasAuthority('SAMPLE_REJECT')")
    @Operation(summary = "Reject a sample")
    public ResponseEntity<Sample> rejectSample(
            @PathVariable Long sampleId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.rejectSample(sampleId, body.get("reason"), u.getUser().getId()));
    }

    @PostMapping("/{sampleId}/archive")
    @PreAuthorize("hasAuthority('SAMPLE_ARCHIVE')")
    @Operation(summary = "Archive a sample")
    public ResponseEntity<Sample> archiveSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.archiveSample(sampleId, u.getUser().getId()));
    }

    // ---- Tests ----

    @PostMapping("/{sampleId}/tests")
    @PreAuthorize("hasAuthority('TEST_ASSIGN')")
    @Operation(summary = "Assign a test to a sample")
    public ResponseEntity<SampleTest> assignTest(
            @PathVariable Long sampleId,
            @RequestBody AssignTestRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.assignTest(sampleId, body, u.getUser().getId()));
    }

    @GetMapping("/{sampleId}/tests")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get all tests for a sample")
    public ResponseEntity<List<SampleTest>> getTestsForSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getTestsForSample(sampleId));
    }

    // ---- Executions ----

    @PostMapping("/tests/{sampleTestId}/executions/start")
    @PreAuthorize("hasAuthority('TEST_EXECUTE')")
    @Operation(summary = "Start a test execution")
    public ResponseEntity<TestExecution> startExecution(
            @PathVariable Long sampleTestId,
            @RequestBody StartExecutionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.startExecution(sampleTestId, body, u.getUser().getId()));
    }

    @PostMapping("/executions/{executionId}/complete")
    @PreAuthorize("hasAuthority('TEST_EXECUTE')")
    @Operation(summary = "Complete a test execution")
    public ResponseEntity<TestExecution> completeExecution(
            @PathVariable Long executionId,
            @RequestBody CompleteExecutionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.completeExecution(executionId, body, u.getUser().getId()));
    }

    @GetMapping("/tests/{sampleTestId}/executions")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get executions for a test")
    public ResponseEntity<List<TestExecution>> getExecutions(
            @PathVariable Long sampleTestId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getExecutionsForTest(sampleTestId));
    }

    // ---- Results ----

    @PostMapping("/tests/{sampleTestId}/results")
    @PreAuthorize("hasAuthority('RESULT_ENTER')")
    @Operation(summary = "Enter a test result")
    public ResponseEntity<TestResult> enterResult(
            @PathVariable Long sampleTestId,
            @RequestBody EnterResultRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.enterResult(sampleTestId, body, u.getUser().getId()));
    }

    @GetMapping("/tests/{sampleTestId}/results")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get results for a test")
    public ResponseEntity<List<TestResult>> getResultsForTest(
            @PathVariable Long sampleTestId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getResultsForTest(sampleTestId));
    }

    @GetMapping("/{sampleId}/results")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get all results for a sample")
    public ResponseEntity<List<TestResult>> getResultsForSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getResultsForSample(sampleId));
    }

    @PostMapping("/results/{resultId}/review")
    @PreAuthorize("hasAuthority('RESULT_REVIEW')")
    @Operation(summary = "Approve a test result")
    public ResponseEntity<TestResult> reviewResult(
            @PathVariable Long resultId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.reviewResult(resultId, u.getUser().getId()));
    }

    @PostMapping("/results/{resultId}/reject")
    @PreAuthorize("hasAuthority('RESULT_REJECT')")
    @Operation(summary = "Reject a test result")
    public ResponseEntity<TestResult> rejectResult(
            @PathVariable Long resultId,
            @RequestBody RejectResultRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.rejectResult(resultId, body, u.getUser().getId()));
    }

    // ---- Release Decision ----

    @PostMapping("/{sampleId}/release")
    @PreAuthorize("hasAuthority('SAMPLE_RELEASE')")
    @Operation(summary = "Make a release decision for a sample")
    public ResponseEntity<ReleaseDecision> makeReleaseDecision(
            @PathVariable Long sampleId,
            @RequestBody ReleaseDecisionRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.makeReleaseDecision(sampleId, body, u.getUser().getId()));
    }

    @GetMapping("/{sampleId}/release")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get release decision for a sample")
    public ResponseEntity<ReleaseDecision> getReleaseDecision(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getReleaseDecision(sampleId));
    }

    // ---- COA ----

    @PostMapping("/{sampleId}/coa/generate")
    @PreAuthorize("hasAuthority('COA_GENERATE')")
    @Operation(summary = "Generate COA for a sample")
    public ResponseEntity<Coa> generateCoa(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.generateCoa(sampleId, u.getUser().getId()));
    }

    @GetMapping("/{sampleId}/coa")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get COA for a sample")
    public ResponseEntity<Coa> getCoaForSample(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getCoaForSample(sampleId));
    }

    @PostMapping("/coa/{coaId}/approve")
    @PreAuthorize("hasAuthority('COA_APPROVE')")
    @Operation(summary = "Approve a COA")
    public ResponseEntity<Coa> approveCoa(
            @PathVariable Long coaId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.approveCoa(coaId, u.getUser().getId()));
    }

    // ---- Sample Types ----

    @PostMapping("/types")
    @PreAuthorize("hasAuthority('SAMPLE_ADMIN')")
    @Operation(summary = "Create a sample type")
    public ResponseEntity<SampleType> createSampleType(
            @RequestBody CreateSampleTypeRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.createSampleType(u.getTenantId(), body));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "List sample types")
    public ResponseEntity<List<SampleType>> getSampleTypes(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSampleTypes(u.getTenantId()));
    }

    // ---- Sample Batches ----

    @PostMapping("/batches")
    @PreAuthorize("hasAuthority('SAMPLE_ADMIN')")
    @Operation(summary = "Create a sample batch")
    public ResponseEntity<SampleBatch> createSampleBatch(
            @RequestBody CreateSampleBatchRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.createSampleBatch(u.getTenantId(), body));
    }

    @GetMapping("/batches")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "List sample batches")
    public ResponseEntity<List<SampleBatch>> getSampleBatches(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSampleBatches(u.getTenantId(), branchId));
    }

    // ---- Test Methods ----

    @PostMapping("/test-methods")
    @PreAuthorize("hasAuthority('SAMPLE_ADMIN')")
    @Operation(summary = "Create a test method")
    public ResponseEntity<TestMethod> createTestMethod(
            @RequestBody CreateTestMethodRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.createTestMethod(u.getTenantId(), body));
    }

    @GetMapping("/test-methods")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "List test methods")
    public ResponseEntity<List<TestMethod>> getTestMethods(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getTestMethods(u.getTenantId()));
    }

    // ---- Specifications ----

    @PostMapping("/specifications")
    @PreAuthorize("hasAuthority('SAMPLE_ADMIN')")
    @Operation(summary = "Create a specification")
    public ResponseEntity<Specification> createSpecification(
            @RequestBody CreateSpecificationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                sampleService.createSpecification(u.getTenantId(), body));
    }

    @GetMapping("/specifications")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get specifications for a product")
    public ResponseEntity<List<Specification>> getSpecifications(
            @RequestParam Long productId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getSpecifications(u.getTenantId(), productId));
    }

    // ---- Attachments ----

    @GetMapping("/{sampleId}/attachments")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get attachments for a sample")
    public ResponseEntity<List<SampleAttachment>> getAttachments(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getAttachments(sampleId));
    }

    // ---- Audit Trail ----

    @GetMapping("/{sampleId}/audit")
    @PreAuthorize("hasAuthority('SAMPLE_VIEW')")
    @Operation(summary = "Get audit trail for a sample")
    public ResponseEntity<List<SampleAuditTrail>> getAuditTrail(
            @PathVariable Long sampleId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(sampleService.getAuditTrail(sampleId));
    }
}
