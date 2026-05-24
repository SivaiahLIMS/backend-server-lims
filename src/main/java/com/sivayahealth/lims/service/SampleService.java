package com.sivayahealth.lims.service;

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

    @Transactional
    public Sample registerSample(Long tenantId, Long branchId, String sampleNo, String sampleType,
                                  String productName, String batchNo, Long createdById) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser creator = userRepository.findById(createdById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        Sample sample = Sample.builder()
                .tenant(tenant).branch(branch)
                .sampleNo(sampleNo).sampleType(sampleType)
                .productName(productName).batchNo(batchNo)
                .receivedAt(LocalDateTime.now())
                .status("REGISTERED").createdBy(creator)
                .build();
        Sample saved = sampleRepository.save(sample);
        auditService.log(tenantId, createdById, "Sample", saved.getId(), "REGISTER", null, sampleNo);
        return saved;
    }

    @Transactional
    public SampleTest assignTest(Long sampleId, Long testDefId, Long assignedToId) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> LimsException.notFound("Sample not found"));
        TestDefinition testDef = testDefinitionRepository.findById(testDefId)
                .orElseThrow(() -> LimsException.notFound("Test definition not found"));
        AppUser assignedTo = userRepository.findById(assignedToId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        SampleTest sampleTest = SampleTest.builder()
                .sample(sample).testDefinition(testDef)
                .assignedTo(assignedTo).status("ASSIGNED")
                .build();
        SampleTest saved = sampleTestRepository.save(sampleTest);
        auditService.log(sample.getTenant().getId(), assignedToId, "SampleTest", saved.getId(),
                "ASSIGN", null, testDef.getCode());
        return saved;
    }

    @Transactional
    public TestResult enterResult(Long sampleTestId, String parameterName, String resultValue,
                                   java.math.BigDecimal numericValue, String unit, Long enteredById) {
        SampleTest sampleTest = sampleTestRepository.findById(sampleTestId)
                .orElseThrow(() -> LimsException.notFound("Sample test not found"));
        AppUser enteredBy = userRepository.findById(enteredById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        TestResult result = TestResult.builder()
                .sampleTest(sampleTest)
                .parameterName(parameterName)
                .resultValue(resultValue)
                .numericValue(numericValue)
                .unit(unit)
                .status("ENTERED")
                .enteredBy(enteredBy)
                .enteredAt(LocalDateTime.now())
                .build();

        sampleTest.setStatus("REVIEW_PENDING");
        sampleTestRepository.save(sampleTest);

        return testResultRepository.save(result);
    }

    @Transactional
    public TestResult reviewResult(Long resultId, Long reviewedById) {
        TestResult result = testResultRepository.findById(resultId)
                .orElseThrow(() -> LimsException.notFound("Result not found"));
        AppUser reviewer = userRepository.findById(reviewedById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        result.setStatus("REVIEWED");
        result.setReviewedBy(reviewer);
        result.setReviewedAt(LocalDateTime.now());
        return testResultRepository.save(result);
    }

    @Transactional
    public Coa generateCoa(Long sampleId, Long tenantId, Long branchId, Long generatedById) {
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> LimsException.notFound("Sample not found"));

        String coaNo = "COA-" + sampleId + "-" + System.currentTimeMillis();

        Coa coa = Coa.builder()
                .sample(sample)
                .tenant(sample.getTenant())
                .branch(sample.getBranch())
                .coaNo(coaNo)
                .status("DRAFT")
                .generatedAt(LocalDateTime.now())
                .build();
        Coa saved = coaRepository.save(coa);
        auditService.log(tenantId, generatedById, "Coa", saved.getId(), "GENERATE", null, coaNo);
        return saved;
    }

    @Transactional
    public Coa approveCoa(Long coaId, Long approvedById) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        AppUser approver = userRepository.findById(approvedById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        coa.setStatus("APPROVED");
        coa.setApprovedBy(approver);
        coa.setApprovedAt(LocalDateTime.now());
        Coa saved = coaRepository.save(coa);
        auditService.log(coa.getTenant().getId(), approvedById, "Coa", coaId, "APPROVE", "DRAFT", "APPROVED");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Sample> getSamples(Long tenantId, Long branchId) {
        return sampleRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }
}
