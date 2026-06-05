package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.qa.*;
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
    private final DeviationNoteRepository deviationNoteRepository;
    private final DeviationActionItemRepository deviationActionItemRepository;
    private final DeviationAuditTrailRepository deviationAuditTrailRepository;

    private final OosCaseRepository oosCaseRepository;
    private final OosNoteRepository oosNoteRepository;
    private final OosActionItemRepository oosActionItemRepository;
    private final OosAuditTrailRepository oosAuditTrailRepository;

    private final CapaRepository capaRepository;
    private final CapaNoteRepository capaNoteRepository;
    private final CapaActionItemRepository capaActionItemRepository;

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    // ── Deviation ────────────────────────────────────────────────────────────

    @Transactional
    public Deviation createDeviation(Long tenantId, Long branchId, CreateDeviationRequest req, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser raisedBy = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        String code = "DEV-" + java.time.Year.now().getValue() + "-" + String.format("%04d", System.currentTimeMillis() % 10000);

        Deviation deviation = Deviation.builder()
                .tenant(tenant).branch(branch)
                .deviationCode(code)
                .refEntity(req.getRefEntity()).refId(req.getRefId())
                .title(req.getTitle())
                .description(req.getDescription())
                .severity(req.getSeverity())
                .sourceType(req.getSourceType())
                .deviationType(req.getDeviationType())
                .status("OPEN").raisedBy(raisedBy)
                .build();
        Deviation saved = deviationRepository.save(deviation);
        logDeviationAudit(saved, "CREATE", null, "OPEN", userId);
        auditService.log(tenantId, userId, "Deviation", saved.getId(), "CREATE", null, "OPEN");
        return saved;
    }

    @Transactional
    public Deviation updateDeviation(Long id, UpdateDeviationRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        if (req.getTitle() != null) deviation.setTitle(req.getTitle());
        if (req.getDescription() != null) deviation.setDescription(req.getDescription());
        if (req.getDeviationType() != null) deviation.setDeviationType(req.getDeviationType());
        return deviationRepository.save(deviation);
    }

    @Transactional
    public Deviation assignDeviationOwner(Long id, AssignDeviationOwnerRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        String old = String.valueOf(deviation.getAssignedTo());
        deviation.setAssignedTo(req.getAssignedTo());
        deviation.setAssignedAt(LocalDateTime.now());
        if ("OPEN".equals(deviation.getStatus())) deviation.setStatus("IN_PROGRESS");
        Deviation saved = deviationRepository.save(deviation);
        logDeviationAudit(saved, "ASSIGN", old, String.valueOf(req.getAssignedTo()), userId);
        return saved;
    }

    @Transactional
    public Deviation updateDeviationStatus(Long id, UpdateDeviationStatusRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        String old = deviation.getStatus();
        deviation.setStatus(req.getStatus());
        Deviation saved = deviationRepository.save(deviation);
        logDeviationAudit(saved, "STATUS_CHANGE", old, req.getStatus(), userId);
        return saved;
    }

    @Transactional
    public Deviation approveDeviation(Long id, ApproveDeviationRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        String old = deviation.getStatus();
        deviation.setStatus("APPROVED");
        deviation.setApprovedBy(userId);
        deviation.setApprovedAt(LocalDateTime.now());
        deviation.setRemarks(req.getRemarks());
        Deviation saved = deviationRepository.save(deviation);
        logDeviationAudit(saved, "APPROVE", old, "APPROVED", userId);
        auditService.log(deviation.getTenant().getId(), userId, "Deviation", id, "APPROVE", old, "APPROVED");
        return saved;
    }

    @Transactional
    public Deviation rejectDeviation(Long id, RejectDeviationRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        String old = deviation.getStatus();
        deviation.setStatus("REJECTED");
        deviation.setRemarks(req.getRemarks());
        Deviation saved = deviationRepository.save(deviation);
        logDeviationAudit(saved, "REJECT", old, "REJECTED", userId);
        return saved;
    }

    @Transactional
    public Deviation closeDeviation(Long id, CloseDeviationRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        AppUser closedBy = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        String old = deviation.getStatus();
        deviation.setStatus("CLOSED");
        deviation.setClosedBy(closedBy);
        deviation.setClosedAt(LocalDateTime.now());
        deviation.setClosureRemarks(req.getRemarks());
        Deviation saved = deviationRepository.save(deviation);
        logDeviationAudit(saved, "CLOSE", old, "CLOSED", userId);
        auditService.log(deviation.getTenant().getId(), userId, "Deviation", id, "CLOSE", old, "CLOSED");
        return saved;
    }

    @Transactional
    public DeviationNote addDeviationNote(Long id, AddDeviationNoteRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        DeviationNote note = DeviationNote.builder()
                .deviation(deviation)
                .noteType(req.getNoteType())
                .text(req.getText())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return deviationNoteRepository.save(note);
    }

    @Transactional
    public DeviationActionItem addDeviationAction(Long id, AddDeviationActionRequest req, Long userId) {
        Deviation deviation = deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
        DeviationActionItem item = DeviationActionItem.builder()
                .deviation(deviation)
                .description(req.getDescription())
                .assignedTo(req.getAssignedTo())
                .assignedAt(LocalDateTime.now())
                .dueDate(req.getDueDate())
                .status("PENDING")
                .build();
        return deviationActionItemRepository.save(item);
    }

    @Transactional
    public DeviationActionItem updateDeviationActionStatus(Long actionId, UpdateDeviationActionStatusRequest req, Long userId) {
        DeviationActionItem item = deviationActionItemRepository.findById(actionId)
                .orElseThrow(() -> LimsException.notFound("Action item not found"));
        item.setStatus(req.getStatus());
        item.setCompletionRemarks(req.getCompletionRemarks());
        if ("COMPLETED".equals(req.getStatus())) item.setCompletedAt(LocalDateTime.now());
        return deviationActionItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<DeviationAuditTrail> getDeviationAuditTrail(Long id) {
        return deviationAuditTrailRepository.findByDeviationIdOrderByPerformedAtAsc(id);
    }

    @Transactional(readOnly = true)
    public List<Deviation> getDeviations(Long tenantId, Long branchId) {
        return deviationRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public Deviation getDeviationById(Long id) {
        return deviationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Deviation not found"));
    }

    // ── OOS ──────────────────────────────────────────────────────────────────

    @Transactional
    public OosCase createOos(Long tenantId, Long branchId, CreateOosRequest req, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));

        String code = "OOS-" + java.time.Year.now().getValue() + "-" + String.format("%04d", System.currentTimeMillis() % 10000);

        OosCase oos = OosCase.builder()
                .tenant(tenant).branch(branch)
                .caseCode(code)
                .sampleId(req.getSampleId()).testId(req.getTestId())
                .oosType(req.getOosType() != null ? req.getOosType() : "OOS")
                .description(req.getDescription())
                .status("OPEN").phase("PHASE_I")
                .raisedBy(userRepository.findById(userId).orElse(null))
                .build();
        OosCase saved = oosCaseRepository.save(oos);
        logOosAudit(saved, "CREATE", null, "OPEN", userId);
        auditService.log(tenantId, userId, "OosCase", saved.getId(), "CREATE", null, "OPEN");
        return saved;
    }

    @Transactional
    public OosCase assignOosInvestigator(Long id, AssignOosInvestigatorRequest req, Long userId) {
        OosCase oos = oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
        String old = String.valueOf(oos.getAssignedTo());
        oos.setAssignedTo(req.getInvestigatorId());
        oos.setAssignedAt(LocalDateTime.now());
        if ("OPEN".equals(oos.getStatus())) oos.setStatus("IN_PROGRESS");
        OosCase saved = oosCaseRepository.save(oos);
        logOosAudit(saved, "ASSIGN_INVESTIGATOR", old, String.valueOf(req.getInvestigatorId()), userId);
        return saved;
    }

    @Transactional
    public OosCase completePhaseI(Long id, CompletePhaseIRequest req, Long userId) {
        OosCase oos = oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
        if (!"PHASE_I".equals(oos.getPhase())) throw LimsException.badRequest("Not in Phase I");
        oos.setPhase("PHASE_II");
        oos.setRootCauseSummary(req.getObservations());
        OosCase saved = oosCaseRepository.save(oos);
        logOosAudit(saved, "PHASE_I_COMPLETE", "PHASE_I", "PHASE_II", userId);
        return saved;
    }

    @Transactional
    public OosCase completePhaseII(Long id, CompletePhaseIIRequest req, Long userId) {
        OosCase oos = oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
        if (!"PHASE_II".equals(oos.getPhase())) throw LimsException.badRequest("Not in Phase II");
        oos.setRootCauseSummary(req.getRootCause());
        oos.setPhase("PHASE_II_COMPLETE");
        OosCase saved = oosCaseRepository.save(oos);
        logOosAudit(saved, "PHASE_II_COMPLETE", "PHASE_II", "PHASE_II_COMPLETE", userId);

        if (req.isCapaRequired()) {
            Tenant tenant = oos.getTenant();
            String capaCode = "CAPA-" + java.time.Year.now().getValue() + "-" + String.format("%04d", System.currentTimeMillis() % 10000);
            Capa capa = Capa.builder()
                    .tenant(tenant)
                    .capaCode(capaCode)
                    .sourceType("OOS")
                    .sourceId(oos.getId())
                    .actionDesc(req.getCapaDescription() != null ? req.getCapaDescription() : "CAPA from OOS: " + oos.getCaseCode())
                    .status("OPEN")
                    .build();
            capaRepository.save(capa);
        }
        return saved;
    }

    @Transactional
    public OosCase closeOosCase(Long id, CloseOosCaseRequest req, Long userId) {
        OosCase oos = oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
        AppUser closedBy = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        String old = oos.getStatus();
        oos.setStatus("CLOSED");
        oos.setPhase("CLOSED");
        oos.setConclusion(req.getConclusion());
        oos.setClosedBy(closedBy);
        oos.setClosedAt(LocalDateTime.now());
        OosCase saved = oosCaseRepository.save(oos);
        logOosAudit(saved, "CLOSE", old, "CLOSED", userId);
        auditService.log(oos.getTenant().getId(), userId, "OosCase", id, "CLOSE", old, "CLOSED");
        return saved;
    }

    @Transactional
    public OosNote addOosNote(Long id, AddOosNoteRequest req, Long userId) {
        OosCase oos = oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
        OosNote note = OosNote.builder()
                .oosCase(oos)
                .noteType(req.getNoteType())
                .text(req.getText())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return oosNoteRepository.save(note);
    }

    @Transactional
    public OosActionItem addOosAction(Long id, AddOosActionRequest req, Long userId) {
        OosCase oos = oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
        OosActionItem item = OosActionItem.builder()
                .oosCase(oos)
                .description(req.getDescription())
                .assignedTo(req.getAssignedTo())
                .assignedAt(LocalDateTime.now())
                .dueDate(req.getDueDate())
                .status("PENDING")
                .build();
        return oosActionItemRepository.save(item);
    }

    @Transactional
    public OosActionItem updateOosActionStatus(Long actionId, UpdateOosActionStatusRequest req, Long userId) {
        OosActionItem item = oosActionItemRepository.findById(actionId)
                .orElseThrow(() -> LimsException.notFound("Action item not found"));
        item.setStatus(req.getStatus());
        item.setCompletionRemarks(req.getCompletionRemarks());
        if ("COMPLETED".equals(req.getStatus())) item.setCompletedAt(LocalDateTime.now());
        return oosActionItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<OosAuditTrail> getOosAuditTrail(Long id) {
        return oosAuditTrailRepository.findByOosCaseIdOrderByPerformedAtAsc(id);
    }

    @Transactional(readOnly = true)
    public List<OosCase> getOosCases(Long tenantId, Long branchId) {
        return oosCaseRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public OosCase getOosCaseById(Long id) {
        return oosCaseRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("OOS case not found"));
    }

    // ── CAPA ─────────────────────────────────────────────────────────────────

    @Transactional
    public Capa createCapa(Long tenantId, CreateCapaRequest req, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Deviation deviation = req.getDeviationId() != null
                ? deviationRepository.findById(req.getDeviationId()).orElse(null) : null;
        AppUser owner = req.getOwnerId() != null
                ? userRepository.findById(req.getOwnerId()).orElse(null) : null;

        String code = "CAPA-" + java.time.Year.now().getValue() + "-" + String.format("%04d", System.currentTimeMillis() % 10000);

        Capa capa = Capa.builder()
                .tenant(tenant).deviation(deviation)
                .capaCode(code)
                .title(req.getTitle())
                .actionDesc(req.getActionDesc())
                .priority(req.getPriority())
                .owner(owner)
                .dueDate(req.getDueDate())
                .status("OPEN")
                .build();
        Capa saved = capaRepository.save(capa);
        auditService.log(tenantId, userId, "Capa", saved.getId(), "CREATE", null, "OPEN");
        return saved;
    }

    @Transactional
    public Capa updateCapa(Long id, UpdateCapaRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        if (req.getTitle() != null) capa.setTitle(req.getTitle());
        if (req.getActionDesc() != null) capa.setActionDesc(req.getActionDesc());
        if (req.getPriority() != null) capa.setPriority(req.getPriority());
        if (req.getDueDate() != null) capa.setDueDate(req.getDueDate());
        return capaRepository.save(capa);
    }

    @Transactional
    public Capa assignCapaOwner(Long id, AssignCapaOwnerRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        AppUser owner = userRepository.findById(req.getAssignedTo())
                .orElseThrow(() -> LimsException.notFound("User not found"));
        capa.setOwner(owner);
        if ("OPEN".equals(capa.getStatus())) capa.setStatus("IN_PROGRESS");
        return capaRepository.save(capa);
    }

    @Transactional
    public Capa updateCapaStatus(Long id, UpdateCapaStatusRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        capa.setStatus(req.getStatus());
        return capaRepository.save(capa);
    }

    @Transactional
    public Capa approveCapa(Long id, ApproveCapaRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        capa.setStatus("APPROVED");
        capa.setApprovedBy(userId);
        capa.setApprovedAt(LocalDateTime.now());
        capa.setRemarks(req.getRemarks());
        return capaRepository.save(capa);
    }

    @Transactional
    public Capa rejectCapa(Long id, RejectCapaRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        capa.setStatus("REJECTED");
        capa.setRemarks(req.getRemarks());
        return capaRepository.save(capa);
    }

    @Transactional
    public Capa closeCapa(Long id, CloseCapaRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        capa.setStatus("CLOSED");
        capa.setClosureRemarks(req.getRemarks());
        capa.setCompletedAt(LocalDateTime.now());
        return capaRepository.save(capa);
    }

    @Transactional
    public CapaNote addCapaNote(Long id, AddCapaNoteRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        CapaNote note = CapaNote.builder()
                .capa(capa)
                .text(req.getText())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return capaNoteRepository.save(note);
    }

    @Transactional
    public CapaActionItem addCapaAction(Long id, AddCapaActionRequest req, Long userId) {
        Capa capa = capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
        CapaActionItem item = CapaActionItem.builder()
                .capa(capa)
                .description(req.getDescription())
                .assignedTo(req.getAssignedTo())
                .assignedAt(LocalDateTime.now())
                .dueDate(req.getDueDate())
                .status("PENDING")
                .build();
        return capaActionItemRepository.save(item);
    }

    @Transactional
    public CapaActionItem updateCapaActionStatus(Long actionId, UpdateCapaActionStatusRequest req, Long userId) {
        CapaActionItem item = capaActionItemRepository.findById(actionId)
                .orElseThrow(() -> LimsException.notFound("Action item not found"));
        item.setStatus(req.getStatus());
        item.setCompletionRemarks(req.getCompletionRemarks());
        if ("COMPLETED".equals(req.getStatus())) item.setCompletedAt(LocalDateTime.now());
        return capaActionItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<Capa> getCapas(Long tenantId) {
        return capaRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public Capa getCapaById(Long id) {
        return capaRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("CAPA not found"));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void logDeviationAudit(Deviation deviation, String action, String oldVal, String newVal, Long userId) {
        DeviationAuditTrail entry = DeviationAuditTrail.builder()
                .deviation(deviation)
                .action(action)
                .oldValueJson(oldVal)
                .newValueJson(newVal)
                .performedBy(userId)
                .performedAt(LocalDateTime.now())
                .build();
        deviationAuditTrailRepository.save(entry);
    }

    private void logOosAudit(OosCase oos, String action, String oldVal, String newVal, Long userId) {
        OosAuditTrail entry = OosAuditTrail.builder()
                .oosCase(oos)
                .action(action)
                .oldValueJson(oldVal)
                .newValueJson(newVal)
                .performedBy(userId)
                .performedAt(LocalDateTime.now())
                .build();
        oosAuditTrailRepository.save(entry);
    }
}
