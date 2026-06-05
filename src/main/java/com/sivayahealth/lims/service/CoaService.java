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
public class CoaService {

    private final CoaRepository coaRepository;
    private final SampleRepository sampleRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Coa generateCoa(Long tenantId, Long branchId, Long sampleId,
                            Long productId, String testResultsJson, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        Sample sample = sampleRepository.findById(sampleId)
                .orElseThrow(() -> LimsException.notFound("Sample not found"));
        AppUser generatedBy = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        String coaNo = "COA-" + java.time.Year.now().getValue() + "-"
                + String.format("%06d", System.currentTimeMillis() % 1000000);

        Coa coa = Coa.builder()
                .tenant(tenant)
                .branch(branch)
                .sample(sample)
                .coaNo(coaNo)
                .productId(productId)
                .testResultsJson(testResultsJson)
                .status("DRAFT")
                .generatedAt(LocalDateTime.now())
                .generatedBy(generatedBy)
                .build();

        Coa saved = coaRepository.save(coa);
        auditService.log(tenantId, userId, "Coa", saved.getId(), "GENERATE", null, coaNo);
        return saved;
    }

    @Transactional
    public Coa approveCoa(Long coaId, Long userId) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        if (!"DRAFT".equals(coa.getStatus()) && !"PENDING_APPROVAL".equals(coa.getStatus())) {
            throw LimsException.badRequest("COA is not in an approvable state");
        }
        AppUser approver = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        coa.setStatus("APPROVED");
        coa.setApprovedBy(approver);
        coa.setApprovedAt(LocalDateTime.now());

        Coa saved = coaRepository.save(coa);
        auditService.log(coa.getTenant().getId(), userId, "Coa", coaId, "APPROVE", "DRAFT", "APPROVED");
        return saved;
    }

    @Transactional
    public Coa submitCoaForApproval(Long coaId, Long userId) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        if (!"DRAFT".equals(coa.getStatus())) {
            throw LimsException.badRequest("Only DRAFT COAs can be submitted for approval");
        }
        coa.setStatus("PENDING_APPROVAL");
        Coa saved = coaRepository.save(coa);
        auditService.log(coa.getTenant().getId(), userId, "Coa", coaId, "SUBMIT", "DRAFT", "PENDING_APPROVAL");
        return saved;
    }

    @Transactional
    public Coa rejectCoa(Long coaId, Long userId) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        coa.setStatus("REJECTED");
        Coa saved = coaRepository.save(coa);
        auditService.log(coa.getTenant().getId(), userId, "Coa", coaId, "REJECT", coa.getStatus(), "REJECTED");
        return saved;
    }

    @Transactional
    public Coa issueCoa(Long coaId, Long userId) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        if (!"APPROVED".equals(coa.getStatus())) {
            throw LimsException.badRequest("Only APPROVED COAs can be issued");
        }
        coa.setStatus("ISSUED");
        Coa saved = coaRepository.save(coa);
        auditService.log(coa.getTenant().getId(), userId, "Coa", coaId, "ISSUE", "APPROVED", "ISSUED");
        return saved;
    }

    @Transactional
    public Coa updateTestResults(Long coaId, String testResultsJson, Long userId) {
        Coa coa = coaRepository.findById(coaId)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
        if (!"DRAFT".equals(coa.getStatus())) {
            throw LimsException.badRequest("Can only update test results for DRAFT COAs");
        }
        coa.setTestResultsJson(testResultsJson);
        return coaRepository.save(coa);
    }

    @Transactional(readOnly = true)
    public List<Coa> getCoasByTenant(Long tenantId) {
        return coaRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<Coa> getCoasByStatus(Long tenantId, String status) {
        return coaRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public Coa getCoaBySample(Long sampleId) {
        return coaRepository.findBySampleId(sampleId)
                .orElseThrow(() -> LimsException.notFound("COA not found for sample"));
    }

    @Transactional(readOnly = true)
    public Coa getCoaById(Long id) {
        return coaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("COA not found"));
    }
}
