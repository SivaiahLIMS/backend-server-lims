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
public class QaService {

    private final DeviationRepository deviationRepository;
    private final OosCaseRepository oosCaseRepository;
    private final CapaRepository capaRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Deviation createDeviation(Long tenantId, Long branchId, String refEntity,
                                      Long refId, String description, String severity, Long raisedById) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser raisedBy = raisedById != null ? userRepository.findById(raisedById).orElse(null) : null;

        Deviation deviation = Deviation.builder()
                .tenant(tenant).branch(branch)
                .refEntity(refEntity).refId(refId)
                .description(description).severity(severity)
                .status("OPEN").raisedBy(raisedBy)
                .build();
        Deviation saved = deviationRepository.save(deviation);
        auditService.log(tenantId, raisedById, "Deviation", saved.getId(), "CREATE", null, severity);
        return saved;
    }

    @Transactional
    public Deviation closeDeviation(Long deviationId, Long closedById, String remarks) {
        Deviation deviation = deviationRepository.findById(deviationId)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        AppUser closedBy = userRepository.findById(closedById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        deviation.setStatus("CLOSED");
        deviation.setClosedBy(closedBy);
        deviation.setClosedAt(LocalDateTime.now());
        deviation.setRemarks(remarks);
        Deviation saved = deviationRepository.save(deviation);
        auditService.log(deviation.getTenant().getId(), closedById, "Deviation", deviationId, "CLOSE", "OPEN", "CLOSED");
        return saved;
    }

    @Transactional
    public OosCase createOos(Long tenantId, Long branchId, Long sampleId, Long testId,
                               String description, Long raisedById) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));

        OosCase oos = OosCase.builder()
                .tenant(tenant).branch(branch)
                .sampleId(sampleId).testId(testId)
                .description(description).status("OPEN")
                .raisedBy(raisedById != null ? userRepository.findById(raisedById).orElse(null) : null)
                .build();
        OosCase saved = oosCaseRepository.save(oos);
        auditService.log(tenantId, raisedById, "OosCase", saved.getId(), "CREATE", null, "OPEN");
        return saved;
    }

    @Transactional
    public Capa createCapa(Long tenantId, Long deviationId, String actionDesc,
                            Long ownerId, java.time.LocalDate dueDate) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Deviation deviation = deviationId != null ? deviationRepository.findById(deviationId).orElse(null) : null;
        AppUser owner = ownerId != null ? userRepository.findById(ownerId).orElse(null) : null;

        Capa capa = Capa.builder()
                .tenant(tenant).deviation(deviation)
                .actionDesc(actionDesc).owner(owner)
                .dueDate(dueDate).status("OPEN")
                .build();
        Capa saved = capaRepository.save(capa);
        auditService.log(tenantId, ownerId, "Capa", saved.getId(), "CREATE", null, "OPEN");
        return saved;
    }

    @Transactional
    public Capa closeCapa(Long capaId, String remarks, Long closedById) {
        Capa capa = capaRepository.findById(capaId)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        capa.setStatus("COMPLETED");
        capa.setCompletedAt(LocalDateTime.now());
        capa.setRemarks(remarks);
        return capaRepository.save(capa);
    }

    @Transactional(readOnly = true)
    public List<Deviation> getDeviations(Long tenantId, Long branchId) {
        return deviationRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<OosCase> getOosCases(Long tenantId, Long branchId) {
        return oosCaseRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<Capa> getCapas(Long tenantId) {
        return capaRepository.findByTenantId(tenantId);
    }
}
