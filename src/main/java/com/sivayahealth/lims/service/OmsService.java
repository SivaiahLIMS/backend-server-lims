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
public class OmsService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderLineRepository poLineRepository;
    private final GoodsReceiptRepository grnRepository;
    private final GoodsReceiptLineRepository grnLineRepository;
    private final SupplierRepository supplierRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public PurchaseOrder createPurchaseOrder(Long tenantId, Long branchId, Long supplierId,
                                              Long createdById, String poNo) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> LimsException.notFound("Supplier not found"));
        AppUser creator = userRepository.findById(createdById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        PurchaseOrder po = PurchaseOrder.builder()
                .tenant(tenant).branch(branch).supplier(supplier)
                .poNo(poNo).status("DRAFT").createdBy(creator)
                .build();
        PurchaseOrder saved = poRepository.save(po);
        auditService.log(tenantId, createdById, "PurchaseOrder", saved.getId(), "CREATE", null, poNo);
        return saved;
    }

    @Transactional
    public PurchaseOrder approvePurchaseOrder(Long poId, Long approverId) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> LimsException.notFound("Purchase order not found"));
        if (!"PENDING_APPROVAL".equals(po.getStatus())) {
            throw LimsException.badRequest("PO is not in PENDING_APPROVAL state");
        }
        AppUser approver = userRepository.findById(approverId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        po.setStatus("APPROVED");
        po.setApprovedBy(approver);
        po.setApprovedAt(LocalDateTime.now());
        PurchaseOrder saved = poRepository.save(po);
        auditService.log(po.getTenant().getId(), approverId, "PurchaseOrder", poId, "APPROVE", "PENDING_APPROVAL", "APPROVED");
        return saved;
    }

    @Transactional
    public GoodsReceipt createGrn(Long tenantId, Long branchId, Long poId,
                                   String grnNo, Long receivedById) {
        PurchaseOrder po = poRepository.findById(poId)
                .orElseThrow(() -> LimsException.notFound("Purchase order not found"));
        if (!"APPROVED".equals(po.getStatus()) && !"SENT".equals(po.getStatus())) {
            throw LimsException.badRequest("PO must be APPROVED or SENT to receive goods");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser receiver = userRepository.findById(receivedById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        GoodsReceipt grn = GoodsReceipt.builder()
                .tenant(tenant).branch(branch).purchaseOrder(po)
                .grnNo(grnNo).receivedBy(receiver)
                .build();
        GoodsReceipt saved = grnRepository.save(grn);
        auditService.log(tenantId, receivedById, "GoodsReceipt", saved.getId(), "CREATE", null, grnNo);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> getPurchaseOrders(Long tenantId, Long branchId) {
        return poRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<GoodsReceipt> getGoodsReceipts(Long tenantId, Long branchId) {
        return grnRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }
}
