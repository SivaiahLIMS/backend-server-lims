package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMasterRepository productMasterRepo;
    private final ProductCompositionRepository compositionRepo;
    private final ProductSpecificationRepository specRepo;
    private final ProductAttachmentRepository attachmentRepo;
    private final ProductWorkflowRepository workflowRepo;
    private final ProductAuditRepository auditRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final AppUserRepository userRepo;
    private final SupplierRepository supplierRepo;
    private final ChemicalMasterRepository chemicalMasterRepo;

    // ── List / Search ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductMaster> list(Long tenantId, Long branchId,
                                    String status, String name, String type) {
        if (status == null && name == null && type == null) {
            return productMasterRepo.findByTenantIdAndBranchId(tenantId, branchId);
        }
        return productMasterRepo.search(tenantId, branchId, status, name, type);
    }

    @Transactional(readOnly = true)
    public ProductMaster getById(Long tenantId, Long branchId, Long productId) {
        ProductMaster p = productMasterRepo.findById(productId)
                .orElseThrow(() -> LimsException.notFound("Product not found"));
        assertScope(p, tenantId, branchId);
        return p;
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public ProductMaster create(Long tenantId, Long branchId, Long userId,
                                ProductMaster data) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));

        if (productMasterRepo.existsByTenant_IdAndProductCode(tenantId, data.getProductCode())) {
            throw LimsException.conflict("Product code already exists: " + data.getProductCode());
        }

        AppUser createdBy = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        data.setTenant(tenant);
        data.setBranch(branch);
        data.setCreatedBy(createdBy);
        data.setStatus("DRAFT");

        ProductMaster saved = productMasterRepo.save(data);
        logWorkflow(saved, tenant, branch, null, "DRAFT", createdBy, "Product created");
        return saved;
    }

    @Transactional
    public ProductMaster update(Long tenantId, Long branchId, Long productId,
                                Long userId, Map<String, Object> fields) {
        ProductMaster p = getById(tenantId, branchId, productId);
        AppUser modifier = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (!"DRAFT".equals(p.getStatus()) && !"REJECTED".equals(p.getStatus())) {
            throw LimsException.badRequest("Only DRAFT or REJECTED products can be edited");
        }

        // Apply and audit each changed field
        applyAndAudit(p, "productName",        fields, modifier);
        applyAndAudit(p, "productType",        fields, modifier);
        applyAndAudit(p, "strength",           fields, modifier);
        applyAndAudit(p, "dosageForm",         fields, modifier);
        applyAndAudit(p, "hsnCode",            fields, modifier);
        applyAndAudit(p, "therapeuticCategory",fields, modifier);
        applyAndAudit(p, "regulatoryStatus",   fields, modifier);
        applyAndAudit(p, "shelfLifeValue",     fields, modifier);
        applyAndAudit(p, "shelfLifeUnit",      fields, modifier);
        applyAndAudit(p, "storageCondition",   fields, modifier);
        applyAndAudit(p, "batchUom",           fields, modifier);
        applyAndAudit(p, "primaryPackaging",   fields, modifier);
        applyAndAudit(p, "secondaryPackaging", fields, modifier);
        applyAndAudit(p, "samplingPlan",       fields, modifier);

        if (fields.containsKey("batchSize")) {
            String old = p.getBatchSize() != null ? p.getBatchSize().toPlainString() : null;
            p.setBatchSize(new BigDecimal(fields.get("batchSize").toString()));
            saveAudit(p, "batchSize", old, p.getBatchSize().toPlainString(), modifier);
        }
        if (fields.containsKey("manufacturerId")) {
            Long mId = ((Number) fields.get("manufacturerId")).longValue();
            Supplier mfr = supplierRepo.findById(mId).orElse(null);
            String old = p.getManufacturer() != null ? p.getManufacturer().getId().toString() : null;
            p.setManufacturer(mfr);
            saveAudit(p, "manufacturerId", old, mId.toString(), modifier);
        }
        if (fields.containsKey("qcReviewerId")) {
            Long uid = ((Number) fields.get("qcReviewerId")).longValue();
            p.setQcReviewer(userRepo.findById(uid).orElse(null));
        }
        if (fields.containsKey("qcManagerId")) {
            Long uid = ((Number) fields.get("qcManagerId")).longValue();
            p.setQcManager(userRepo.findById(uid).orElse(null));
        }

        p.setModifiedBy(modifier);
        p.setModifiedAt(LocalDateTime.now());
        return productMasterRepo.save(p);
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @Transactional
    public ProductMaster submit(Long tenantId, Long branchId, Long productId, Long userId) {
        return transition(tenantId, branchId, productId, userId, "DRAFT", "UNDER_REVIEW",
                null, "Submitted for review");
    }

    @Transactional
    public ProductMaster approve(Long tenantId, Long branchId, Long productId,
                                 Long userId, String comments) {
        return transition(tenantId, branchId, productId, userId,
                "UNDER_REVIEW", "APPROVED", comments, comments);
    }

    @Transactional
    public ProductMaster reject(Long tenantId, Long branchId, Long productId,
                                Long userId, String comments) {
        return transition(tenantId, branchId, productId, userId,
                "UNDER_REVIEW", "REJECTED", comments, comments);
    }

    @Transactional(readOnly = true)
    public List<ProductWorkflow> getWorkflowHistory(Long tenantId, Long branchId, Long productId) {
        assertExists(tenantId, branchId, productId);
        return workflowRepo.findByProductProductIdAndTenantIdAndBranchIdOrderByActionAtAsc(
                productId, tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<ProductAudit> getAuditTrail(Long tenantId, Long branchId, Long productId) {
        assertExists(tenantId, branchId, productId);
        return auditRepo.findByProductProductIdAndTenantIdAndBranchIdOrderByChangedAtDesc(
                productId, tenantId, branchId);
    }

    // ── Composition (BOM) ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductComposition> getComposition(Long tenantId, Long branchId, Long productId) {
        assertExists(tenantId, branchId, productId);
        return compositionRepo.findByProductProductIdAndTenantIdAndBranchId(productId, tenantId, branchId);
    }

    @Transactional
    public ProductComposition addIngredient(Long tenantId, Long branchId, Long productId,
                                            Long ingredientId, BigDecimal qty,
                                            String uom, String grade, Long userId) {
        ProductMaster product = getById(tenantId, branchId, productId);
        ChemicalMaster ingredient = chemicalMasterRepo.findById(ingredientId)
                .orElseThrow(() -> LimsException.notFound("Chemical/ingredient not found"));
        AppUser creator = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        Tenant tenant = product.getTenant();
        Branch branch = product.getBranch();

        return compositionRepo.save(ProductComposition.builder()
                .product(product)
                .tenant(tenant)
                .branch(branch)
                .ingredient(ingredient)
                .ingredientQuantity(qty)
                .ingredientUom(uom)
                .ingredientGrade(grade)
                .createdBy(creator)
                .build());
    }

    @Transactional
    public void removeIngredient(Long tenantId, Long branchId, Long productId, Long itemId) {
        assertExists(tenantId, branchId, productId);
        ProductComposition item = compositionRepo.findById(itemId)
                .orElseThrow(() -> LimsException.notFound("Composition item not found"));
        if (!item.getTenant().getId().equals(tenantId) || !item.getBranch().getId().equals(branchId)) {
            throw LimsException.forbidden("Access denied");
        }
        compositionRepo.delete(item);
    }

    // ── Specification ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ProductSpecification getSpec(Long tenantId, Long branchId, Long productId) {
        assertExists(tenantId, branchId, productId);
        return specRepo.findByProductProductIdAndTenantIdAndBranchId(productId, tenantId, branchId)
                .orElse(null);
    }

    @Transactional
    public ProductSpecification upsertSpec(Long tenantId, Long branchId, Long productId,
                                           Long userId, String specDocPath, String testMethods,
                                           String releaseCriteria, String stabilityReqs) {
        ProductMaster product = getById(tenantId, branchId, productId);
        AppUser user = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        ProductSpecification spec = specRepo.findByProductProductIdAndTenantIdAndBranchId(
                        productId, tenantId, branchId)
                .orElseGet(() -> ProductSpecification.builder()
                        .product(product)
                        .tenant(product.getTenant())
                        .branch(product.getBranch())
                        .createdBy(user)
                        .build());

        spec.setSpecDocumentPath(specDocPath);
        spec.setTestMethods(testMethods);
        spec.setReleaseCriteria(releaseCriteria);
        spec.setStabilityRequirements(stabilityReqs);
        spec.setModifiedBy(user);
        spec.setModifiedAt(LocalDateTime.now());
        return specRepo.save(spec);
    }

    // ── Attachments ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ProductAttachment> getAttachments(Long tenantId, Long branchId, Long productId) {
        assertExists(tenantId, branchId, productId);
        return attachmentRepo.findByProductProductIdAndTenantIdAndBranchId(productId, tenantId, branchId);
    }

    @Transactional
    public ProductAttachment addAttachment(Long tenantId, Long branchId, Long productId,
                                           String fileName, String fileType, String filePath,
                                           Long userId) {
        ProductMaster product = getById(tenantId, branchId, productId);
        AppUser uploader = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        return attachmentRepo.save(ProductAttachment.builder()
                .product(product)
                .tenant(product.getTenant())
                .branch(product.getBranch())
                .fileName(fileName)
                .fileType(fileType)
                .filePath(filePath)
                .uploadedBy(uploader)
                .build());
    }

    @Transactional
    public void deleteAttachment(Long tenantId, Long branchId, Long productId, Long attachmentId) {
        assertExists(tenantId, branchId, productId);
        ProductAttachment att = attachmentRepo.findByAttachmentIdAndTenantIdAndBranchId(
                        attachmentId, tenantId, branchId)
                .orElseThrow(() -> LimsException.notFound("Attachment not found"));
        attachmentRepo.delete(att);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ProductMaster transition(Long tenantId, Long branchId, Long productId, Long userId,
                                     String from, String to, String comment, String logComment) {
        ProductMaster p = getById(tenantId, branchId, productId);
        if (!from.equals(p.getStatus())) {
            throw LimsException.badRequest("Expected status " + from + " but found " + p.getStatus());
        }
        AppUser actor = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        String old = p.getStatus();
        p.setStatus(to);
        if ("APPROVED".equals(to) || "REJECTED".equals(to)) {
            p.setApprovalComments(comment);
        } else if ("UNDER_REVIEW".equals(to)) {
            p.setReviewComments(comment);
        }
        p.setModifiedBy(actor);
        p.setModifiedAt(LocalDateTime.now());
        p = productMasterRepo.save(p);

        logWorkflow(p, p.getTenant(), p.getBranch(), old, to, actor, logComment);
        return p;
    }

    private void logWorkflow(ProductMaster p, Tenant tenant, Branch branch,
                              String oldStatus, String newStatus, AppUser actor, String comments) {
        workflowRepo.save(ProductWorkflow.builder()
                .product(p)
                .tenant(tenant)
                .branch(branch)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .actionBy(actor)
                .comments(comments)
                .build());
    }

    private void saveAudit(ProductMaster p, String field, String oldVal, String newVal, AppUser by) {
        if (oldVal == null && newVal == null) return;
        if (oldVal != null && oldVal.equals(newVal)) return;
        auditRepo.save(ProductAudit.builder()
                .product(p)
                .tenant(p.getTenant())
                .branch(p.getBranch())
                .fieldName(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .changedBy(by)
                .build());
    }

    @SuppressWarnings("unchecked")
    private void applyAndAudit(ProductMaster p, String fieldName,
                               Map<String, Object> fields, AppUser modifier) {
        if (!fields.containsKey(fieldName)) return;
        String newVal = fields.get(fieldName) != null ? fields.get(fieldName).toString() : null;
        String oldVal;
        try {
            var getter = ProductMaster.class.getMethod(
                    "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1));
            Object v = getter.invoke(p);
            oldVal = v != null ? v.toString() : null;
            if (oldVal != null && oldVal.equals(newVal)) return;
            var setter = ProductMaster.class.getMethod(
                    "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1), String.class);
            setter.invoke(p, newVal);
            saveAudit(p, fieldName, oldVal, newVal, modifier);
        } catch (Exception ignored) {
            // field doesn't match getter/setter — skip silently
        }
    }

    private void assertScope(ProductMaster p, Long tenantId, Long branchId) {
        if (!p.getTenant().getId().equals(tenantId) || !p.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Product not found");
        }
    }

    private void assertExists(Long tenantId, Long branchId, Long productId) {
        getById(tenantId, branchId, productId);
    }
}
