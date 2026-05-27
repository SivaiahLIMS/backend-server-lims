package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderRequestService {

    private final OrderRequestRepository orderRequestRepository;
    private final OrderRequestHistoryRepository historyRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final ChemicalMasterRepository chemicalMasterRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final SupplierRepository supplierRepository;
    private final UomDetailsRepository uomRepository;
    private final AuditService auditService;

    // ── Queries / Lists ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<OrderRequest> getAll(Long tenantId, Long branchId) {
        return branchId != null
                ? orderRequestRepository.findByTenantIdAndBranchId(tenantId, branchId)
                : orderRequestRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public List<OrderRequest> getByBranchAndStatus(Long tenantId, Long branchId, String status) {
        return orderRequestRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
    }

    @Transactional(readOnly = true)
    public List<OrderRequest> getByStatus(Long tenantId, String status) {
        return orderRequestRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional(readOnly = true)
    public List<OrderRequest> getDueForDelivery(Long tenantId, int daysAhead) {
        return orderRequestRepository.findDueForDelivery(tenantId, LocalDate.now().plusDays(daysAhead));
    }

    @Transactional(readOnly = true)
    public OrderRequest getById(Long id) {
        return orderRequestRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Order request not found"));
    }

    @Transactional(readOnly = true)
    public List<OrderRequestHistory> getHistory(Long orderRequestId) {
        return historyRepository.findByOrderRequestIdOrderByChangedAtAsc(orderRequestId);
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public OrderRequest create(Long tenantId, Long branchId, Long requestedById,
                               String requestType, Long chemicalId, Long instrumentId,
                               BigDecimal quantity, Long uomId, String reason,
                               Long supplierId, LocalDate requiredByDate) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser requestedBy = userRepository.findById(requestedById)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        ChemicalMaster chemical = null;
        InstrumentMaster instrument = null;

        if ("CHEMICAL".equals(requestType)) {
            if (chemicalId == null) throw LimsException.badRequest("chemicalId is required for CHEMICAL requests");
            chemical = chemicalMasterRepository.findById(chemicalId)
                    .orElseThrow(() -> LimsException.notFound("Chemical not found"));
        } else if ("INSTRUMENT".equals(requestType)) {
            if (instrumentId == null) throw LimsException.badRequest("instrumentId is required for INSTRUMENT requests");
            instrument = instrumentMasterRepository.findById(instrumentId)
                    .orElseThrow(() -> LimsException.notFound("Instrument not found"));
        } else {
            throw LimsException.badRequest("requestType must be CHEMICAL or INSTRUMENT");
        }

        Supplier supplier = supplierId != null
                ? supplierRepository.findById(supplierId).orElse(null) : null;
        UomDetails uom = uomId != null
                ? uomRepository.findById(uomId).orElse(null) : null;

        OrderRequest req = OrderRequest.builder()
                .tenant(tenant)
                .branch(branch)
                .requestType(requestType)
                .chemical(chemical)
                .instrument(instrument)
                .quantity(quantity)
                .uom(uom)
                .reason(reason)
                .status("DRAFT")
                .requestedBy(requestedBy)
                .supplier(supplier)
                .requiredByDate(requiredByDate)
                .build();

        req = orderRequestRepository.save(req);
        recordHistory(req, null, "DRAFT", requestedBy, "Created");
        auditService.log(tenantId, requestedById, "OrderRequest", req.getId(), "CREATE", null, requestType);
        return req;
    }

    // ── Lifecycle transitions ────────────────────────────────────────────────

    @Transactional
    public OrderRequest submit(Long id, Long userId) {
        return transition(id, userId, "DRAFT", "SUBMITTED", "Submitted for approval", or -> {
            or.setSubmittedAt(LocalDateTime.now());
        });
    }

    @Transactional
    public OrderRequest approve(Long id, Long userId, String comment) {
        return transition(id, userId, "SUBMITTED", "APPROVED", comment, or -> {
            or.setApprovedBy(userRepository.findById(userId).orElse(null));
            or.setApprovedAt(LocalDateTime.now());
        });
    }

    @Transactional
    public OrderRequest reject(Long id, Long userId, String comment) {
        OrderRequest req = orderRequestRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Order request not found"));
        if (!"SUBMITTED".equals(req.getStatus())) {
            throw LimsException.badRequest("Only SUBMITTED requests can be rejected");
        }
        String old = req.getStatus();
        req.setStatus("DRAFT");
        req = orderRequestRepository.save(req);
        AppUser user = userRepository.findById(userId).orElse(null);
        recordHistory(req, old, "DRAFT", user, comment != null ? comment : "Rejected");
        auditService.log(req.getTenant().getId(), userId, "OrderRequest", req.getId(), "REJECT", old, "DRAFT");
        return req;
    }

    @Transactional
    public OrderRequest placeOrder(Long id, Long userId, String poNumber,
                                   Long supplierId, LocalDate expectedDeliveryDate, String notes) {
        return transition(id, userId, "APPROVED", "ORDER_PLACED", notes, or -> {
            or.setPoNumber(poNumber);
            if (supplierId != null) or.setSupplier(supplierRepository.findById(supplierId).orElse(null));
            or.setExpectedDeliveryDate(expectedDeliveryDate);
        });
    }

    @Transactional
    public OrderRequest markReceived(Long id, Long userId,
                                     BigDecimal deliveredQuantity, String deliveryNotes) {
        return transition(id, userId, "ORDER_PLACED", "RECEIVED", deliveryNotes, or -> {
            or.setDeliveredQuantity(deliveredQuantity);
            or.setDeliveredAt(LocalDateTime.now());
            or.setDeliveryNotes(deliveryNotes);
        });
    }

    @Transactional
    public OrderRequest close(Long id, Long userId, String comment) {
        return transition(id, userId, "RECEIVED", "CLOSED", comment, or -> {});
    }

    // ── Internal helpers ─────────────────────────────────────────────────────

    @FunctionalInterface
    private interface Mutator {
        void apply(OrderRequest req);
    }

    private OrderRequest transition(Long id, Long userId, String expectedFrom,
                                    String newStatus, String comment, Mutator mutator) {
        OrderRequest req = orderRequestRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Order request not found"));
        if (!expectedFrom.equals(req.getStatus())) {
            throw LimsException.badRequest(
                    "Expected status " + expectedFrom + " but found " + req.getStatus());
        }
        String old = req.getStatus();
        mutator.apply(req);
        req.setStatus(newStatus);
        req = orderRequestRepository.save(req);
        AppUser user = userRepository.findById(userId).orElse(null);
        recordHistory(req, old, newStatus, user, comment);
        auditService.log(req.getTenant().getId(), userId, "OrderRequest", req.getId(),
                newStatus, old, newStatus);
        return req;
    }

    private void recordHistory(OrderRequest req, String oldStatus, String newStatus,
                               AppUser changedBy, String comment) {
        historyRepository.save(OrderRequestHistory.builder()
                .orderRequest(req)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .changedBy(changedBy)
                .changedAt(LocalDateTime.now())
                .comment(comment)
                .build());
    }

}
