package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WorksheetService {

    private final WorksheetMasterRepository worksheetRepo;
    private final WorksheetExecutionDataRepository execDataRepo;
    private final WorksheetReviewHistoryRepository reviewHistoryRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final AppUserRepository userRepo;
    private final ProductMasterRepository productMasterRepo;
    private final DocumentMasterRepository documentMasterRepo;
    private final ChemicalMasterRepository chemicalMasterRepo;
    private final InstrumentMasterRepository instrumentMasterRepo;

    // ── List / Search ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorksheetMaster> listAll(Long tenantId, Long branchId) {
        return worksheetRepo.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<WorksheetMaster> listByStatus(Long tenantId, Long branchId, String status) {
        return worksheetRepo.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
    }

    @Transactional(readOnly = true)
    public List<WorksheetMaster> listArchived(Long tenantId, Long branchId) {
        return worksheetRepo.findByTenantIdAndBranchIdAndIsArchived(tenantId, branchId, true);
    }

    @Transactional(readOnly = true)
    public List<WorksheetMaster> listAssignedTo(Long tenantId, Long branchId, Long userId) {
        return worksheetRepo.findByTenantIdAndBranchIdAndAssignedTo_Id(tenantId, branchId, userId);
    }

    @Transactional(readOnly = true)
    public List<WorksheetMaster> search(Long tenantId, Long branchId,
                                        String status, Boolean isArchived,
                                        Long productId, Long assignedToId,
                                        String batchNo, LocalDateTime from, LocalDateTime to) {
        return worksheetRepo.search(tenantId, branchId, status, isArchived,
                productId, assignedToId, batchNo, from, to);
    }

    @Transactional(readOnly = true)
    public WorksheetMaster getById(Long tenantId, Long branchId, Long worksheetId) {
        WorksheetMaster w = worksheetRepo.findById(worksheetId)
                .orElseThrow(() -> LimsException.notFound("Worksheet not found"));
        assertScope(w, tenantId, branchId);
        return w;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetMaster create(Long tenantId, Long branchId, Long userId,
                                  WorksheetMaster data) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser creator = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        data.setTenant(tenant);
        data.setBranch(branch);
        data.setCreatedBy(creator);
        data.setStatus("DRAFT");
        data.setArchived(false);

        WorksheetMaster saved = worksheetRepo.save(data);
        logHistory(saved, null, "DRAFT", creator, "Worksheet created");
        return saved;
    }

    @Transactional
    public WorksheetMaster update(Long tenantId, Long branchId, Long worksheetId,
                                  Long userId, Map<String, Object> fields) {
        WorksheetMaster w = getById(tenantId, branchId, worksheetId);
        if (!"DRAFT".equals(w.getStatus()) && !"REJECTED".equals(w.getStatus())) {
            throw LimsException.badRequest("Only DRAFT or REJECTED worksheets can be edited");
        }
        AppUser modifier = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (fields.containsKey("batchNo")) {
            w.setBatchNo((String) fields.get("batchNo"));
        }
        if (fields.containsKey("productId") && fields.get("productId") != null) {
            Long pid = ((Number) fields.get("productId")).longValue();
            ProductMaster product = productMasterRepo.findById(pid)
                    .orElseThrow(() -> LimsException.notFound("Product not found"));
            w.setProduct(product);
        }
        if (fields.containsKey("templateId") && fields.get("templateId") != null) {
            Long tid = ((Number) fields.get("templateId")).longValue();
            DocumentMaster template = documentMasterRepo.findById(tid)
                    .orElseThrow(() -> LimsException.notFound("Template document not found"));
            w.setTemplate(template);
        }

        w.setModifiedBy(modifier);
        w.setModifiedAt(LocalDateTime.now());
        return worksheetRepo.save(w);
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetMaster assign(Long tenantId, Long branchId, Long worksheetId,
                                  Long assignToUserId, Long assignedByUserId) {
        WorksheetMaster w = getById(tenantId, branchId, worksheetId);
        AppUser assignee = userRepo.findById(assignToUserId)
                .orElseThrow(() -> LimsException.notFound("Assignee user not found"));
        AppUser assigner = userRepo.findById(assignedByUserId)
                .orElseThrow(() -> LimsException.notFound("Assigner user not found"));

        w.setAssignedTo(assignee);
        w.setAssignedBy(assigner);
        w.setModifiedBy(assigner);
        w.setModifiedAt(LocalDateTime.now());
        return worksheetRepo.save(w);
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetMaster submit(Long tenantId, Long branchId, Long worksheetId, Long userId) {
        return transition(tenantId, branchId, worksheetId, userId, "DRAFT", "SUBMITTED", null);
    }

    @Transactional
    public WorksheetMaster startReview(Long tenantId, Long branchId, Long worksheetId, Long userId) {
        return transition(tenantId, branchId, worksheetId, userId, "SUBMITTED", "UNDER_REVIEW", null);
    }

    @Transactional
    public WorksheetMaster approve(Long tenantId, Long branchId, Long worksheetId,
                                   Long userId, String comments) {
        return transition(tenantId, branchId, worksheetId, userId, "UNDER_REVIEW", "APPROVED", comments);
    }

    @Transactional
    public WorksheetMaster reject(Long tenantId, Long branchId, Long worksheetId,
                                  Long userId, String comments) {
        return transition(tenantId, branchId, worksheetId, userId, "UNDER_REVIEW", "REJECTED", comments);
    }

    @Transactional
    public WorksheetMaster close(Long tenantId, Long branchId, Long worksheetId,
                                 Long userId, String comments) {
        return transition(tenantId, branchId, worksheetId, userId, "APPROVED", "CLOSED", comments);
    }

    // ── Archive ───────────────────────────────────────────────────────────────

    @Transactional
    public WorksheetMaster archive(Long tenantId, Long branchId, Long worksheetId, Long userId) {
        WorksheetMaster w = getById(tenantId, branchId, worksheetId);
        AppUser actor = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (w.isArchived()) {
            throw LimsException.badRequest("Worksheet is already archived");
        }
        w.setArchived(true);
        w.setArchivedAt(LocalDateTime.now());
        w.setModifiedBy(actor);
        w.setModifiedAt(LocalDateTime.now());
        logHistory(w, w.getStatus(), w.getStatus(), actor, "Archived");
        return worksheetRepo.save(w);
    }

    @Transactional
    public WorksheetMaster unarchive(Long tenantId, Long branchId, Long worksheetId, Long userId) {
        WorksheetMaster w = getById(tenantId, branchId, worksheetId);
        AppUser actor = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        w.setArchived(false);
        w.setArchivedAt(null);
        w.setModifiedBy(actor);
        w.setModifiedAt(LocalDateTime.now());
        logHistory(w, w.getStatus(), w.getStatus(), actor, "Unarchived");
        return worksheetRepo.save(w);
    }

    // ── Execution Data ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorksheetExecutionData> getExecutionData(Long tenantId, Long branchId,
                                                          Long worksheetId) {
        assertExists(tenantId, branchId, worksheetId);
        return execDataRepo.findByWorksheet_WorksheetIdAndTenantIdAndBranchId(
                worksheetId, tenantId, branchId);
    }

    @Transactional
    public WorksheetExecutionData saveExecutionData(Long tenantId, Long branchId,
                                                     Long worksheetId, Long userId,
                                                     Map<String, Object> body) {
        WorksheetMaster worksheet = getById(tenantId, branchId, worksheetId);
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        Long fieldId = body.containsKey("fieldId") && body.get("fieldId") != null
                ? ((Number) body.get("fieldId")).longValue() : null;
        String fieldName = body.containsKey("fieldName") ? (String) body.get("fieldName") : null;
        String value     = body.containsKey("value")     ? (String) body.get("value")     : null;
        String unit      = body.containsKey("unit")      ? (String) body.get("unit")      : null;
        String comment   = body.containsKey("comment")   ? (String) body.get("comment")   : null;
        String reason    = body.containsKey("reason")    ? (String) body.get("reason")    : null;

        ChemicalMaster chemical = null;
        if (body.containsKey("chemicalId") && body.get("chemicalId") != null) {
            Long cid = ((Number) body.get("chemicalId")).longValue();
            chemical = chemicalMasterRepo.findById(cid).orElse(null);
        }

        InstrumentMaster instrument = null;
        if (body.containsKey("instrumentId") && body.get("instrumentId") != null) {
            Long iid = ((Number) body.get("instrumentId")).longValue();
            instrument = instrumentMasterRepo.findById(iid).orElse(null);
        }

        return execDataRepo.save(WorksheetExecutionData.builder()
                .worksheet(worksheet)
                .tenant(worksheet.getTenant())
                .branch(worksheet.getBranch())
                .fieldId(fieldId)
                .fieldName(fieldName)
                .value(value)
                .unit(unit)
                .chemical(chemical)
                .instrument(instrument)
                .comment(comment)
                .reason(reason)
                .createdBy(user)
                .build());
    }

    @Transactional
    public void replaceExecutionData(Long tenantId, Long branchId, Long worksheetId,
                                     Long userId, List<Map<String, Object>> rows) {
        WorksheetMaster worksheet = getById(tenantId, branchId, worksheetId);
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        execDataRepo.deleteByWorksheet_WorksheetIdAndTenantIdAndBranchId(
                worksheetId, tenantId, branchId);

        for (Map<String, Object> row : rows) {
            Long fieldId   = row.containsKey("fieldId") && row.get("fieldId") != null
                    ? ((Number) row.get("fieldId")).longValue() : null;
            String fieldName = row.containsKey("fieldName") ? (String) row.get("fieldName") : null;
            String value     = row.containsKey("value")     ? (String) row.get("value")     : null;
            String unit      = row.containsKey("unit")      ? (String) row.get("unit")      : null;
            String comment   = row.containsKey("comment")   ? (String) row.get("comment")   : null;
            String reason    = row.containsKey("reason")    ? (String) row.get("reason")    : null;

            ChemicalMaster chemical = null;
            if (row.containsKey("chemicalId") && row.get("chemicalId") != null) {
                chemical = chemicalMasterRepo.findById(
                        ((Number) row.get("chemicalId")).longValue()).orElse(null);
            }
            InstrumentMaster instrument = null;
            if (row.containsKey("instrumentId") && row.get("instrumentId") != null) {
                instrument = instrumentMasterRepo.findById(
                        ((Number) row.get("instrumentId")).longValue()).orElse(null);
            }

            execDataRepo.save(WorksheetExecutionData.builder()
                    .worksheet(worksheet)
                    .tenant(worksheet.getTenant())
                    .branch(worksheet.getBranch())
                    .fieldId(fieldId)
                    .fieldName(fieldName)
                    .value(value)
                    .unit(unit)
                    .chemical(chemical)
                    .instrument(instrument)
                    .comment(comment)
                    .reason(reason)
                    .createdBy(user)
                    .build());
        }
    }

    // ── Review History ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<WorksheetReviewHistory> getReviewHistory(Long tenantId, Long branchId,
                                                          Long worksheetId) {
        assertExists(tenantId, branchId, worksheetId);
        return reviewHistoryRepo.findByWorksheet_WorksheetIdAndTenantIdAndBranchIdOrderByActionAtAsc(
                worksheetId, tenantId, branchId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private WorksheetMaster transition(Long tenantId, Long branchId, Long worksheetId,
                                       Long userId, String from, String to, String comments) {
        WorksheetMaster w = getById(tenantId, branchId, worksheetId);
        if (!from.equals(w.getStatus())) {
            throw LimsException.badRequest(
                    "Expected status " + from + " but found " + w.getStatus());
        }
        AppUser actor = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        String oldStatus = w.getStatus();
        w.setStatus(to);
        w.setModifiedBy(actor);
        w.setModifiedAt(LocalDateTime.now());
        w = worksheetRepo.save(w);
        logHistory(w, oldStatus, to, actor, comments);
        return w;
    }

    private void logHistory(WorksheetMaster w, String oldStatus, String newStatus,
                             AppUser actor, String comments) {
        reviewHistoryRepo.save(WorksheetReviewHistory.builder()
                .worksheet(w)
                .tenant(w.getTenant())
                .branch(w.getBranch())
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actionBy(actor)
                .comments(comments)
                .build());
    }

    private void assertScope(WorksheetMaster w, Long tenantId, Long branchId) {
        if (!w.getTenant().getId().equals(tenantId) || !w.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Worksheet not found");
        }
    }

    private void assertExists(Long tenantId, Long branchId, Long worksheetId) {
        getById(tenantId, branchId, worksheetId);
    }
}
