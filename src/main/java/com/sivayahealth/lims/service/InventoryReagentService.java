package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryReagentService {

    private final InventoryReagentRepository reagentRepository;
    private final InventoryReagentLotRepository lotRepository;
    private final InventoryMovementRepository movementRepository;
    private final TenantRepository tenantRepository;
    private final SupplierRepository supplierRepository;
    private final AppUserRepository appUserRepository;
    private final AuditService auditService;
    private final ReagentInventoryDispatcher dispatcher;

    @Transactional
    public InventoryReagent createReagent(Long tenantId, Long branchId, String name, String category,
                                          String formula, String defaultUom, BigDecimal minStockLevel,
                                          BigDecimal reorderLevel, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));

        String code = "RGT-" + java.time.Year.now().getValue()
                + "-" + String.format("%05d", System.currentTimeMillis() % 100000);

        InventoryReagent reagent = InventoryReagent.builder()
                .tenant(tenant)
                .branchId(branchId)
                .reagentCode(code)
                .name(name)
                .category(category)
                .formula(formula)
                .defaultUom(defaultUom)
                .minStockLevel(minStockLevel)
                .reorderLevel(reorderLevel)
                .status("ACTIVE")
                .createdBy(userId)
                .build();

        InventoryReagent saved = reagentRepository.save(reagent);
        auditService.log(tenantId, userId, "InventoryReagent", saved.getId(), "CREATE", null, name);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryReagent> getReagents(Long tenantId, Long branchId) {
        if (branchId != null) return reagentRepository.findByTenantIdAndBranchId(tenantId, branchId);
        return reagentRepository.findByTenantId(tenantId);
    }

    @Transactional(readOnly = true)
    public InventoryReagent getReagentById(Long tenantId, Long id) {
        InventoryReagent reagent = reagentRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Reagent not found"));
        if (!reagent.getTenant().getId().equals(tenantId)) throw LimsException.notFound("Reagent not found");
        return reagent;
    }

    @Transactional
    public InventoryReagentLot createLot(Long tenantId, Long reagentId, String lotNumber, String supplierLot,
                                         BigDecimal receivedQty, String uom, LocalDate receivedDate,
                                         LocalDate expiryDate, LocalDate manufactureDate, Long supplierId,
                                         String storageLocation, String certificateNo, Long userId) {
        if (receivedQty == null || receivedQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw LimsException.badRequest("Received quantity must be greater than zero");
        }

        InventoryReagent reagent = reagentRepository.findById(reagentId)
                .orElseThrow(() -> LimsException.notFound("Reagent not found"));
        if (!reagent.getTenant().getId().equals(tenantId)) throw LimsException.notFound("Reagent not found");

        if (lotRepository.findByLotNumber(lotNumber).isPresent()) {
            throw LimsException.badRequest("Lot number already exists: " + lotNumber);
        }

        Supplier supplier = null;
        if (supplierId != null) {
            supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> LimsException.notFound("Supplier not found"));
        }

        InventoryReagentLot lot = InventoryReagentLot.builder()
                .reagent(reagent)
                .lotNumber(lotNumber)
                .supplierLot(supplierLot)
                .receivedQty(receivedQty)
                .currentQty(receivedQty)
                .uom(uom != null ? uom : reagent.getDefaultUom())
                .receivedDate(receivedDate)
                .expiryDate(expiryDate)
                .manufactureDate(manufactureDate)
                .supplier(supplier)
                .storageLocation(storageLocation)
                .certificateNo(certificateNo)
                .status("AVAILABLE")
                .receivedBy(userId)
                .build();

        InventoryReagentLot saved = lotRepository.save(lot);

        AppUser performer = appUserRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        InventoryMovement movement = InventoryMovement.builder()
                .lot(saved)
                .movementType("RECEIPT")
                .quantity(receivedQty)
                .qtyBefore(BigDecimal.ZERO)
                .qtyAfter(receivedQty)
                .reason("Initial receipt")
                .performedBy(performer)
                .build();
        movementRepository.save(movement);

        auditService.log(tenantId, userId, "InventoryReagentLot", saved.getId(), "CREATE", null, lotNumber);

        dispatcher.scheduleExpiryCheckTask(saved, tenantId, reagent.getBranchId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryReagentLot> getLotsByReagent(Long reagentId) {
        return lotRepository.findByReagentId(reagentId);
    }

    @Transactional(readOnly = true)
    public List<InventoryReagentLot> getAvailableLotsFEFO(Long reagentId) {
        return lotRepository.findAvailableByReagentFEFO(reagentId);
    }

    @Transactional
    public InventoryMovement consumeLot(Long tenantId, Long lotId, BigDecimal quantity, String reason,
                                        String refEntity, Long refId, Long userId) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw LimsException.badRequest("Quantity must be greater than zero");
        }

        InventoryReagentLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> LimsException.notFound("Lot not found"));

        InventoryReagent reagent = lot.getReagent();
        Long lotTenantId = reagent.getTenant().getId();
        if (!lotTenantId.equals(tenantId)) throw LimsException.notFound("Lot not found");

        if (!"AVAILABLE".equals(lot.getStatus())) {
            throw LimsException.badRequest("Lot is not available for consumption");
        }
        if (lot.getCurrentQty().compareTo(quantity) < 0) {
            throw LimsException.badRequest("Insufficient quantity. Available: " + lot.getCurrentQty());
        }

        BigDecimal before = lot.getCurrentQty();
        BigDecimal after = before.subtract(quantity);
        lot.setCurrentQty(after);
        if (after.compareTo(BigDecimal.ZERO) == 0) lot.setStatus("EXHAUSTED");
        lotRepository.save(lot);

        AppUser performer = appUserRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        InventoryMovement movement = InventoryMovement.builder()
                .lot(lot)
                .movementType("CONSUME")
                .quantity(quantity)
                .qtyBefore(before)
                .qtyAfter(after)
                .refEntity(refEntity)
                .refId(refId)
                .reason(reason)
                .performedBy(performer)
                .build();

        InventoryMovement saved = movementRepository.save(movement);
        auditService.log(lotTenantId, userId, "InventoryReagentLot", lotId, "CONSUME",
                before.toPlainString(), after.toPlainString());

        dispatcher.checkReorderThreshold(reagent, lotTenantId, reagent.getBranchId());

        return saved;
    }

    @Transactional
    public InventoryMovement adjustLot(Long tenantId, Long lotId, BigDecimal quantity, String movementType,
                                       String reason, Long userId) {
        if (movementType == null) throw LimsException.badRequest("movementType is required");
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) < 0) {
            throw LimsException.badRequest("Quantity must be zero or greater");
        }

        InventoryReagentLot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> LimsException.notFound("Lot not found"));

        InventoryReagent reagent = lot.getReagent();
        Long lotTenantId = reagent.getTenant().getId();
        if (!lotTenantId.equals(tenantId)) throw LimsException.notFound("Lot not found");

        BigDecimal before = lot.getCurrentQty();
        BigDecimal after = switch (movementType.toUpperCase()) {
            case "ADD" -> before.add(quantity);
            case "SUBTRACT" -> {
                if (before.compareTo(quantity) < 0) throw LimsException.badRequest("Insufficient quantity");
                yield before.subtract(quantity);
            }
            case "SET" -> quantity;
            default -> throw LimsException.badRequest("Invalid movementType. Use ADD, SUBTRACT, or SET");
        };

        lot.setCurrentQty(after);
        if (after.compareTo(BigDecimal.ZERO) == 0) lot.setStatus("EXHAUSTED");
        else if ("EXHAUSTED".equals(lot.getStatus())) lot.setStatus("AVAILABLE");
        lotRepository.save(lot);

        AppUser performer = appUserRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        InventoryMovement movement = InventoryMovement.builder()
                .lot(lot)
                .movementType("ADJUSTMENT_" + movementType.toUpperCase())
                .quantity(quantity)
                .qtyBefore(before)
                .qtyAfter(after)
                .reason(reason)
                .performedBy(performer)
                .build();

        InventoryMovement saved = movementRepository.save(movement);
        auditService.log(lotTenantId, userId, "InventoryReagentLot", lotId, "ADJUST",
                before.toPlainString(), after.toPlainString());

        String op = movementType.toUpperCase();
        if ("SUBTRACT".equals(op) || "SET".equals(op)) {
            dispatcher.checkReorderThreshold(reagent, lotTenantId, reagent.getBranchId());
        }

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InventoryMovement> getMovements(Long lotId) {
        return movementRepository.findByLotIdOrderByPerformedAtDesc(lotId);
    }

    @Transactional(readOnly = true)
    public List<InventoryReagentLot> getExpiringLots(Long tenantId, int daysAhead) {
        return lotRepository.findExpiringLots(tenantId, LocalDate.now().plusDays(daysAhead));
    }
}
