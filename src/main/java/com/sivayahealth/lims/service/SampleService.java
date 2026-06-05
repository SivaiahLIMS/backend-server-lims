package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.sample.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleRepository sampleRepository;
    private final SampleTestRepository sampleTestRepository;
    private final TestResultRepository testResultRepository;
    private final CoaRepository coaRepository;
    private final TestDefinitionRepository testDefinitionRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;
    private final SampleTypeRepository sampleTypeRepository;
    private final SampleBatchRepository sampleBatchRepository;
    private final TestMethodRepository testMethodRepository;
    private final SpecificationRepository specificationRepository;
    private final TestAssignmentRepository testAssignmentRepository;
    private final TestExecutionRepository testExecutionRepository;
    private final ReleaseDecisionRepository releaseDecisionRepository;
    private final SampleAttachmentRepository sampleAttachmentRepository;
    private final SampleAuditTrailRepository sampleAuditTrailRepository;

    // ---- Sample Registration & Lifecycle ----

    @Transactional
    public Sample registerSample(Long tenantId, RegisterSampleRequest req, Long createdById) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(req.getBranchId())
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser creator = userRepository.findById(createdById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (sampleRepository.existsByTenantIdAndSampleNo(tenantId, req.getSampleNo())) {
            throw LimsException.conflict("Sample number already exists: " + req.getSampleNo());
        }

        SampleType sampleTypeRef = null;
        if (req.getSampleTypeId() != null) {
            sampleTypeRef = sampleTypeRepository.findById(req.getSampleTypeId())
                    .orElseThrow(() -> LimsException.notFound("Sample type not found"));
        }

        SampleBatch sampleBatch = null;
        if (req.getSampleBatchId() != null) {
            sampleBatch = sampleBatchRepository.findById(req.getSampleBatchId())
                    .orElseThrow(() -> LimsException.notFound("Sample batch not found"));
        }

        Sample sample = Sample.builder()
                .tenant(tenant).branch(branch)
                .sampleNo(req.getSampleNo())
                .sampleCode(req.getSampleCode())
                .sampleTypeRef(sampleTypeRef)
                .sampleType(req.getSampleType())
                .productId(req.getProductId())
                .productName(req.getProductName())
                .batchNo(req.getBatchNo())
                .sampleBatch(sampleBatch)
                .quantity(req.getQuantity())
                .unit(req.getUnit())
                .dueDate(req.getDueDate())
                .priority(req.getPriority() != null ? req.getPriority() : 0)
                .storageLocationId(req.getStorageLocationId())
                .receivedAt(LocalDateTime.now())
                .status(SampleStatus.REGISTERED)
                .createdBy(creator)
                .build();

        Sample saved = sampleRepository.save(sample);
        logAuditTrail(saved, "REGISTERED", null, req.getSampleNo(), creator);
        auditService.log(tenantId, createdById, "Sample", saved.getId(), "REGISTER", null, req.getSampleNo());
        return saved;
    }

    @Transactional
    public Sample receiveSample(Long sampleId, Long userId) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser user = getUserOrThrow(userId);
        if (sample.getStatus() != SampleStatus.REGISTERED) {
            throw LimsException.badRequest("Sample must be in REGISTERED status to receive");
        }
        String oldStatus = sample.getStatus().name();
        sample.setStatus(SampleStatus.RECEIVED);
        sample.setReceivedAt(LocalDateTime.now());
        Sample saved = sampleRepository.save(sample);
        logAuditTrail(saved, "RECEIVED", oldStatus, SampleStatus.RECEIVED.name(), user);
        auditService.log(sample.getTenant().getId(), userId, "Sample", sampleId, "RECEIVE", oldStatus, "RECEIVED");
        return saved;
    }

    @Transactional
    public Sample approveSample(Long sampleId, Long userId) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser user = getUserOrThrow(userId);
        String oldStatus = sample.getStatus().name();
        sample.setStatus(SampleStatus.APPROVED);
        Sample saved = sampleRepository.save(sample);
        logAuditTrail(saved, "APPROVED", oldStatus, SampleStatus.APPROVED.name(), user);
        auditService.log(sample.getTenant().getId(), userId, "Sample", sampleId, "APPROVE", oldStatus, "APPROVED");
        return saved;
    }

    @Transactional
    public Sample rejectSample(Long sampleId, String reason, Long userId) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser user = getUserOrThrow(userId);
        String oldStatus = sample.getStatus().name();
        sample.setStatus(SampleStatus.REJECTED);
        Sample saved = sampleRepository.save(sample);
        logAuditTrail(saved, "REJECTED", oldStatus, reason, user);
        auditService.log(sample.getTenant().getId(), userId, "Sample", sampleId, "REJECT", oldStatus, reason);
        return saved;
    }

    @Transactional
    public Sample archiveSample(Long sampleId, Long userId) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser user = getUserOrThrow(userId);
        String oldStatus = sample.getStatus().name();
        sample.setStatus(SampleStatus.ARCHIVED);
        Sample saved = sampleRepository.save(sample);
        logAuditTrail(saved, "ARCHIVED", oldStatus, SampleStatus.ARCHIVED.name(), user);
        auditService.log(sample.getTenant().getId(), userId, "Sample", sampleId, "ARCHIVE", oldStatus, "ARCHIVED");
        return saved;
    }

    // ---- Test Assignment ----

    @Transactional
    public SampleTest assignTest(Long sampleId, AssignTestRequest req, Long assignedById) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser assignedTo = getUserOrThrow(req.getAssignedToId());

        TestDefinition testDef = null;
        if (req.getTestDefId() != null) {
            testDef = testDefinitionRepository.findById(req.getTestDefId())
                    .orElseThrow(() -> LimsException.notFound("Test definition not found"));
        }

        TestMethod testMethod = null;
        if (req.getTestMethodId() != null) {
            testMethod = testMethodRepository.findById(req.getTestMethodId())
                    .orElseThrow(() -> LimsException.notFound("Test method not found"));
        }

        SampleTest sampleTest = SampleTest.builder()
                .sample(sample)
                .testDefinition(testDef)
                .testMethod(testMethod)
                .assignedTo(assignedTo)
                .assignedAt(LocalDateTime.now())
                .dueDate(req.getDueDate())
                .status(TestStatus.ASSIGNED)
                .build();

        SampleTest saved = sampleTestRepository.save(sampleTest);

        AppUser assigner = getUserOrThrow(assignedById);
        TestAssignment assignment = TestAssignment.builder()
                .sampleTest(saved)
                .analyst(assignedTo)
                .assignedAt(LocalDateTime.now())
                .assignedBy(assigner)
                .dueDate(req.getDueDate())
                .build();
        testAssignmentRepository.save(assignment);

        if (sample.getStatus() == SampleStatus.RECEIVED) {
            sample.setStatus(SampleStatus.IN_PROGRESS);
            sampleRepository.save(sample);
        }

        auditService.log(sample.getTenant().getId(), assignedById, "SampleTest", saved.getId(),
                "ASSIGN", null, assignedTo.getUsername());
        return saved;
    }

    // ---- Test Execution ----

    @Transactional
    public TestExecution startExecution(Long sampleTestId, StartExecutionRequest req, Long userId) {
        SampleTest sampleTest = getSampleTestOrThrow(sampleTestId);
        AppUser user = getUserOrThrow(userId);

        sampleTest.setStatus(TestStatus.IN_PROGRESS);
        sampleTest.setStartedAt(LocalDateTime.now());
        sampleTestRepository.save(sampleTest);

        TestExecution execution = TestExecution.builder()
                .sampleTest(sampleTest)
                .instrumentId(req.getInstrumentId())
                .startTime(LocalDateTime.now())
                .comments(req.getComments())
                .executedBy(user)
                .build();
        return testExecutionRepository.save(execution);
    }

    @Transactional
    public TestExecution completeExecution(Long executionId, CompleteExecutionRequest req, Long userId) {
        TestExecution execution = testExecutionRepository.findById(executionId)
                .orElseThrow(() -> LimsException.notFound("Test execution not found"));
        getUserOrThrow(userId);

        execution.setExecutionDataJson(req.getExecutionDataJson());
        execution.setComments(req.getComments());
        execution.setEndTime(LocalDateTime.now());
        TestExecution saved = testExecutionRepository.save(execution);

        SampleTest sampleTest = execution.getSampleTest();
        sampleTest.setStatus(TestStatus.COMPLETED);
        sampleTest.setCompletedAt(LocalDateTime.now());
        sampleTestRepository.save(sampleTest);

        return saved;
    }

    // ---- Test Results ----

    @Transactional
    public TestResult enterResult(Long sampleTestId, EnterResultRequest req, Long enteredById) {
        SampleTest sampleTest = getSampleTestOrThrow(sampleTestId);
        AppUser enteredBy = getUserOrThrow(enteredById);

        TestResult result = TestResult.builder()
                .sampleTest(sampleTest)
                .parameterName(req.getParameterName())
                .resultValue(req.getResultValue())
                .numericValue(req.getNumericValue())
                .unit(req.getUnit())
                .qualifier(req.getQualifier())
                .remarks(req.getRemarks())
                .status("ENTERED")
                .enteredBy(enteredBy)
                .enteredAt(LocalDateTime.now())
                .build();

        sampleTest.setStatus(TestStatus.REVIEW_PENDING);
        sampleTestRepository.save(sampleTest);

        return testResultRepository.save(result);
    }

    @Transactional
    public TestResult reviewResult(Long resultId, Long reviewedById) {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> LimsException.notFound("Result not found"));
        AppUser reviewer = getUserOrThrow(reviewedById);

        result.setStatus("REVIEWED");
        result.setReviewedBy(reviewer);
        result.setReviewedAt(LocalDateTime.now());

        SampleTest sampleTest = result.getSampleTest();
        sampleTest.setStatus(TestStatus.APPROVED);
        sampleTestRepository.save(sampleTest);

        return testResultRepository.save(result);
    }

    @Transactional
    public TestResult rejectResult(Long resultId, RejectResultRequest req, Long rejectedById) {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> LimsException.notFound("Result not found"));
        AppUser rejector = getUserOrThrow(rejectedById);

        result.setStatus("REJECTED");
        result.setReviewedBy(rejector);
        result.setReviewedAt(LocalDateTime.now());
        result.setRemarks(req.getReason());

        SampleTest sampleTest = result.getSampleTest();
        sampleTest.setStatus(TestStatus.REJECTED);
        sampleTestRepository.save(sampleTest);

        return testResultRepository.save(result);
    }

    // ---- Release Decision ----

    @Transactional
    public ReleaseDecision makeReleaseDecision(Long sampleId, ReleaseDecisionRequest req, Long userId) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser user = getUserOrThrow(userId);

        releaseDecisionRepository.findBySampleId(sampleId).ifPresent(existing -> {
            throw LimsException.conflict("Release decision already exists for this sample");
        });

        ReleaseDecision decision = ReleaseDecision.builder()
                .sample(sample)
                .decision(req.getDecision())
                .decidedBy(user)
                .decidedAt(LocalDateTime.now())
                .reason(req.getReason())
                .build();

        String oldStatus = sample.getStatus().name();
        if (req.getDecision() == ReleaseStatus.RELEASED) {
            sample.setStatus(SampleStatus.APPROVED);
        } else if (req.getDecision() == ReleaseStatus.REJECTED) {
            sample.setStatus(SampleStatus.REJECTED);
        }
        sampleRepository.save(sample);

        ReleaseDecision saved = releaseDecisionRepository.save(decision);
        logAuditTrail(sample, "RELEASE_DECISION", oldStatus, req.getDecision().name(), user);
        auditService.log(sample.getTenant().getId(), userId, "Sample", sampleId,
                "RELEASE_DECISION", oldStatus, req.getDecision().name());
        return saved;
    }

    @Transactional(readOnly = true)
    public ReleaseDecision getReleaseDecision(Long sampleId) {
        return releaseDecisionRepository.findBySampleId(sampleId)
                .orElseThrow(() -> LimsException.notFound("No release decision for this sample"));
    }

    // ---- COA ----

    @Transactional
    public Coa generateCoa(Long sampleId, Long generatedById) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser generator = getUserOrThrow(generatedById);

        coaRepository.findBySampleId(sampleId).ifPresent(existing -> {
            throw LimsException.conflict("COA already exists for this sample");
        });

        List<TestResult> results = testResultRepository.findBySampleTestSampleId(sampleId);
        String coaNo = "COA-" + sampleId + "-" + System.currentTimeMillis();

        Coa coa = Coa.builder()
                .sample(sample)
                .tenant(sample.getTenant())
                .branch(sample.getBranch())
                .coaNo(coaNo)
                .productId(sample.getProductId())
                .status("DRAFT")
                .generatedAt(LocalDateTime.now())
                .generatedBy(generator)
                .build();
        Coa saved = coaRepository.save(coa);
        auditService.log(sample.getTenant().getId(), generatedById, "Coa", saved.getId(), "GENERATE", null, coaNo);
        return saved;
    }

    @Transactional
    public Coa approveCoa(Long coaId, Long approvedById) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        AppUser approver = getUserOrThrow(approvedById);

        coa.setStatus("APPROVED");
        coa.setApprovedBy(approver);
        coa.setApprovedAt(LocalDateTime.now());
        Coa saved = coaRepository.save(coa);
        auditService.log(coa.getTenant().getId(), approvedById, "Coa", coaId, "APPROVE", "DRAFT", "APPROVED");
        return saved;
    }

    // ---- Sample Types ----

    @Transactional
    public SampleType createSampleType(Long tenantId, CreateSampleTypeRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        sampleTypeRepository.findByTenantIdAndName(tenantId, req.getName()).ifPresent(existing -> {
            throw LimsException.conflict("Sample type already exists: " + req.getName());
        });
        SampleType sampleType = SampleType.builder()
                .tenant(tenant)
                .name(req.getName())
                .description(req.getDescription())
                .defaultTestMethodIds(req.getDefaultTestMethodIds())
                .active(true)
                .build();
        return sampleTypeRepository.save(sampleType);
    }

    @Transactional(readOnly = true)
    public List<SampleType> getSampleTypes(Long tenantId) {
        return sampleTypeRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    // ---- Sample Batches ----

    @Transactional
    public SampleBatch createSampleBatch(Long tenantId, CreateSampleBatchRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(req.getBranchId())
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        sampleBatchRepository.findByTenantIdAndBatchNo(tenantId, req.getBatchNo()).ifPresent(existing -> {
            throw LimsException.conflict("Batch number already exists: " + req.getBatchNo());
        });
        SampleBatch batch = SampleBatch.builder()
                .tenant(tenant).branch(branch)
                .productId(req.getProductId())
                .batchNo(req.getBatchNo())
                .manufactureDate(req.getManufactureDate())
                .expiryDate(req.getExpiryDate())
                .status("ACTIVE")
                .build();
        return sampleBatchRepository.save(batch);
    }

    @Transactional(readOnly = true)
    public List<SampleBatch> getSampleBatches(Long tenantId, Long branchId) {
        return sampleBatchRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    // ---- Test Methods ----

    @Transactional
    public TestMethod createTestMethod(Long tenantId, CreateTestMethodRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        TestMethod method = TestMethod.builder()
                .tenant(tenant)
                .name(req.getName())
                .description(req.getDescription())
                .sopDocumentId(req.getSopDocumentId())
                .version(req.getVersion())
                .active(true)
                .build();
        return testMethodRepository.save(method);
    }

    @Transactional(readOnly = true)
    public List<TestMethod> getTestMethods(Long tenantId) {
        return testMethodRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    // ---- Specifications ----

    @Transactional
    public Specification createSpecification(Long tenantId, CreateSpecificationRequest req) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        specificationRepository.findByTenantIdAndProductIdAndTestMethodId(tenantId, req.getProductId(), req.getTestMethodId())
                .ifPresent(existing -> {
                    throw LimsException.conflict("Specification already exists for this product/test method combination");
                });
        Specification spec = Specification.builder()
                .tenant(tenant)
                .productId(req.getProductId())
                .testMethodId(req.getTestMethodId())
                .minValue(req.getMinValue())
                .maxValue(req.getMaxValue())
                .targetValue(req.getTargetValue())
                .unit(req.getUnit())
                .ootLower(req.getOotLower())
                .ootUpper(req.getOotUpper())
                .oosLower(req.getOosLower())
                .oosUpper(req.getOosUpper())
                .active(true)
                .build();
        return specificationRepository.save(spec);
    }

    @Transactional(readOnly = true)
    public List<Specification> getSpecifications(Long tenantId, Long productId) {
        return specificationRepository.findByTenantIdAndProductId(tenantId, productId);
    }

    // ---- Attachments ----

    @Transactional
    public SampleAttachment addAttachment(Long sampleId, String filePath, String originalFilename,
                                           String fileType, Long fileSize, Long uploadedById) {
        Sample sample = getSampleOrThrow(sampleId);
        AppUser uploader = getUserOrThrow(uploadedById);
        SampleAttachment attachment = SampleAttachment.builder()
                .sample(sample)
                .filePath(filePath)
                .originalFilename(originalFilename)
                .fileType(fileType)
                .fileSize(fileSize)
                .uploadedBy(uploader)
                .uploadedAt(LocalDateTime.now())
                .build();
        return sampleAttachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<SampleAttachment> getAttachments(Long sampleId) {
        getSampleOrThrow(sampleId);
        return sampleAttachmentRepository.findBySampleId(sampleId);
    }

    // ---- Audit Trail ----

    @Transactional(readOnly = true)
    public List<SampleAuditTrail> getAuditTrail(Long sampleId) {
        getSampleOrThrow(sampleId);
        return sampleAuditTrailRepository.findBySampleIdOrderByPerformedAtDesc(sampleId);
    }

    // ---- Queries ----

    @Transactional(readOnly = true)
    public Sample getSample(Long sampleId) {
        return getSampleOrThrow(sampleId);
    }

    @Transactional(readOnly = true)
    public List<Sample> getSamples(Long tenantId, Long branchId) {
        if (branchId != null) {
            return sampleRepository.findByTenantIdAndBranchId(tenantId, branchId);
        }
        return sampleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Sample> getSamplesByStatus(Long tenantId, SampleStatus status) {
        return sampleRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public List<SampleTest> getTestsForSample(Long sampleId) {
        getSampleOrThrow(sampleId);
        return sampleTestRepository.findBySampleId(sampleId);
    }

    @Transactional(readOnly = true)
    public List<TestResult> getResultsForTest(Long sampleTestId) {
        getSampleTestOrThrow(sampleTestId);
        return testResultRepository.findBySampleTestId(sampleTestId);
    }

    @Transactional(readOnly = true)
    public List<TestResult> getResultsForSample(Long sampleId) {
        getSampleOrThrow(sampleId);
        return testResultRepository.findBySampleTestSampleId(sampleId);
    }

    @Transactional(readOnly = true)
    public List<TestExecution> getExecutionsForTest(Long sampleTestId) {
        getSampleTestOrThrow(sampleTestId);
        return testExecutionRepository.findBySampleTestId(sampleTestId);
    }

    @Transactional(readOnly = true)
    public Coa getCoaForSample(Long sampleId) {
        return coaRepository.findBySampleId(sampleId)
                .orElseThrow(() -> LimsException.notFound("No COA for this sample"));
    }

    // ---- Helpers ----

    private Sample getSampleOrThrow(Long sampleId) {
        return sampleRepository.findById(sampleId)
                .orElseThrow(() -> LimsException.notFound("Sample not found"));
    }

    private SampleTest getSampleTestOrThrow(Long sampleTestId) {
        return sampleTestRepository.findById(sampleTestId)
                .orElseThrow(() -> LimsException.notFound("Sample test not found"));
    }

    private AppUser getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
    }

    private void logAuditTrail(Sample sample, String action, String oldValue, String newValue, AppUser performedBy) {
        SampleAuditTrail trail = SampleAuditTrail.builder()
                .sample(sample)
                .action(action)
                .oldValueJson(oldValue)
                .newValueJson(newValue)
                .performedBy(performedBy)
                .performedAt(LocalDateTime.now())
                .build();
        sampleAuditTrailRepository.save(trail);
    }
}
