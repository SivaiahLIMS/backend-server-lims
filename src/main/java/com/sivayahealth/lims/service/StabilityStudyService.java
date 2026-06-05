package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StabilityStudyService {

    private final StabilityStudyRepository studyRepository;
    private final StabilityStudyTimepointRepository timepointRepository;
    private final StabilityStudyResultRepository resultRepository;
    private final TenantRepository tenantRepository;
    private final ProductMasterRepository productRepository;
    private final AuditService auditService;

    @Transactional
    public StabilityStudy createStudy(Long tenantId, Long branchId, Long productId,
                                       String title, String studyType, String protocol,
                                       String storageCondition, LocalDate startDate, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));

        ProductMaster product = productRepository.findById(productId)
                .orElseThrow(() -> LimsException.notFound("Product not found"));

        String code = "STB-" + java.time.Year.now().getValue() + "-"
                + String.format("%05d", System.currentTimeMillis() % 100000);

        StabilityStudy study = StabilityStudy.builder()
                .tenant(tenant)
                .branchId(branchId)
                .studyCode(code)
                .product(product)
                .title(title)
                .studyType(studyType)
                .protocol(protocol)
                .storageCondition(storageCondition)
                .startDate(startDate)
                .status("DRAFT")
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();

        StabilityStudy saved = studyRepository.save(study);
        auditService.log(tenantId, userId, "StabilityStudy", saved.getId(), "CREATE", null, code);
        return saved;
    }

    @Transactional
    public StabilityStudy activateStudy(Long studyId, Long userId) {
        StabilityStudy study = getStudy(studyId);
        if (!"DRAFT".equals(study.getStatus())) throw LimsException.badRequest("Only DRAFT studies can be activated");
        study.setStatus("ACTIVE");
        StabilityStudy saved = studyRepository.save(study);
        auditService.log(study.getTenant().getId(), userId, "StabilityStudy", studyId, "ACTIVATE", "DRAFT", "ACTIVE");
        return saved;
    }

    @Transactional
    public StabilityStudy completeStudy(Long studyId, LocalDate endDate, Long userId) {
        StabilityStudy study = getStudy(studyId);
        if (!"ACTIVE".equals(study.getStatus())) throw LimsException.badRequest("Only ACTIVE studies can be completed");
        study.setStatus("COMPLETED");
        study.setEndDate(endDate != null ? endDate : LocalDate.now());
        StabilityStudy saved = studyRepository.save(study);
        auditService.log(study.getTenant().getId(), userId, "StabilityStudy", studyId, "COMPLETE", "ACTIVE", "COMPLETED");
        return saved;
    }

    @Transactional
    public StabilityStudy discontinueStudy(Long studyId, Long userId) {
        StabilityStudy study = getStudy(studyId);
        study.setStatus("DISCONTINUED");
        study.setEndDate(LocalDate.now());
        StabilityStudy saved = studyRepository.save(study);
        auditService.log(study.getTenant().getId(), userId, "StabilityStudy", studyId, "DISCONTINUE", study.getStatus(), "DISCONTINUED");
        return saved;
    }

    @Transactional
    public StabilityStudyTimepoint addTimepoint(Long studyId, String timepoint,
                                                 LocalDate scheduledDate, Long userId) {
        StabilityStudy study = getStudy(studyId);
        StabilityStudyTimepoint tp = StabilityStudyTimepoint.builder()
                .study(study)
                .timepoint(timepoint)
                .scheduledDate(scheduledDate)
                .status("PENDING")
                .build();
        return timepointRepository.save(tp);
    }

    @Transactional
    public StabilityStudyResult recordResult(Long timepointId, String parameter,
                                              String specification, String result,
                                              String passFail, String remarks, Long userId) {
        StabilityStudyTimepoint tp = timepointRepository.findById(timepointId)
                .orElseThrow(() -> LimsException.notFound("Timepoint not found"));

        StabilityStudyResult res = StabilityStudyResult.builder()
                .timepoint(tp)
                .parameter(parameter)
                .specification(specification)
                .result(result)
                .passFail(passFail)
                .testedBy(userId)
                .testedAt(LocalDateTime.now())
                .remarks(remarks)
                .build();
        return resultRepository.save(res);
    }

    @Transactional
    public StabilityStudyTimepoint completeTimepoint(Long timepointId, Long userId) {
        StabilityStudyTimepoint tp = timepointRepository.findById(timepointId)
                .orElseThrow(() -> LimsException.notFound("Timepoint not found"));
        tp.setStatus("COMPLETED");
        tp.setCompletedDate(LocalDate.now());
        tp.setCompletedBy(userId);
        return timepointRepository.save(tp);
    }

    @Transactional(readOnly = true)
    public List<StabilityStudy> getStudies(Long tenantId, Long branchId) {
        return studyRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<StabilityStudy> getStudiesByStatus(Long tenantId, Long branchId, String status) {
        return studyRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
    }

    @Transactional(readOnly = true)
    public List<StabilityStudy> getStudiesByProduct(Long productId) {
        return studyRepository.findByProduct_ProductId(productId);
    }

    @Transactional(readOnly = true)
    public StabilityStudy getStudyById(Long id) {
        return getStudy(id);
    }

    @Transactional(readOnly = true)
    public List<StabilityStudyTimepoint> getTimepoints(Long studyId) {
        return timepointRepository.findByStudyId(studyId);
    }

    @Transactional(readOnly = true)
    public List<StabilityStudyResult> getResults(Long timepointId) {
        return resultRepository.findByTimepointId(timepointId);
    }

    @Transactional(readOnly = true)
    public List<StabilityStudyResult> getAllResults(Long studyId) {
        return resultRepository.findByTimepointStudyId(studyId);
    }

    private StabilityStudy getStudy(Long id) {
        return studyRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Stability study not found"));
    }
}
