package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dispatcher that bridges inventory events to the ScheduledTask engine and PredictiveAlert system.
 *
 * Responsibilities:
 *  - On lot creation: schedule a REAGENT_EXPIRY_CHECK task for lots with an expiry date
 *  - On consumption: check if total available stock fell below reorder threshold
 *  - On scheduler run: expire overdue lots, raise reorder alerts, raise expiry alerts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReagentInventoryDispatcher {

    private final InventoryReagentRepository reagentRepository;
    private final InventoryReagentLotRepository lotRepository;
    private final ScheduledTaskRepository scheduledTaskRepository;
    private final PredictiveAlertRepository predictiveAlertRepository;

    // ── Auto-Expiry Task Creation ────────────────────────────────────────────────

    /**
     * Called immediately after a lot is received.
     * Creates a REAGENT_EXPIRY_CHECK ScheduledTask 7 days before the expiry date.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void scheduleExpiryCheckTask(InventoryReagentLot lot, Long tenantId, Long branchId) {
        if (lot.getExpiryDate() == null) return;

        LocalDate checkDate = lot.getExpiryDate().minusDays(7);
        if (checkDate.isBefore(LocalDate.now())) {
            checkDate = LocalDate.now(); // already within 7-day window — schedule immediately
        }

        boolean alreadyScheduled = scheduledTaskRepository
                .findByRefEntityAndRefId("InventoryReagentLot", lot.getId())
                .stream()
                .anyMatch(t -> t.getTaskType() == TaskType.REAGENT_EXPIRY_CHECK
                        && t.getStatus() == TaskStatusEnum.PENDING);

        if (alreadyScheduled) return;

        ScheduledTask task = ScheduledTask.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .taskType(TaskType.REAGENT_EXPIRY_CHECK)
                .title("Expiry check: " + lot.getReagent().getName() + " (Lot " + lot.getLotNumber() + ")")
                .description("Lot expires on " + lot.getExpiryDate()
                        + ". Verify quantity, update status, or dispose as required.")
                .refEntity("InventoryReagentLot")
                .refId(lot.getId())
                .dueDate(checkDate)
                .status(TaskStatusEnum.PENDING)
                .build();

        scheduledTaskRepository.save(task);
        log.info("[Dispatcher] Scheduled REAGENT_EXPIRY_CHECK task for lot {} due {}", lot.getLotNumber(), checkDate);
    }

    // ── Auto-Reorder Check ───────────────────────────────────────────────────────

    /**
     * Called after each consumption.
     * Raises a PredictiveAlert (LOW_STOCK) if total available qty < reorderLevel.
     * Optionally creates a REORDER ScheduledTask.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkReorderThreshold(InventoryReagent reagent, Long tenantId, Long branchId) {
        if (reagent.getReorderLevel() == null) return;

        BigDecimal totalAvailable = lotRepository.findAvailableByReagentFEFO(reagent.getId())
                .stream()
                .map(InventoryReagentLot::getCurrentQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAvailable.compareTo(reagent.getReorderLevel()) > 0) return;

        // Raise LOW_STOCK predictive alert if none already open
        boolean alertOpen = predictiveAlertRepository.existsByEntityTypeAndEntityIdAndStatus(
                "InventoryReagent", reagent.getId(), "OPEN");

        if (!alertOpen) {
            PredictiveAlert alert = PredictiveAlert.builder()
                    .tenantId(tenantId)
                    .branchId(branchId)
                    .alertType("LOW_STOCK")
                    .entityType("InventoryReagent")
                    .entityId(reagent.getId())
                    .message("Reagent '" + reagent.getName() + "' (" + reagent.getReagentCode()
                            + ") is below reorder level. Available: " + totalAvailable
                            + " " + reagent.getDefaultUom() + " / Reorder at: "
                            + reagent.getReorderLevel() + " " + reagent.getDefaultUom())
                    .severity(totalAvailable.compareTo(reagent.getMinStockLevel() != null
                            ? reagent.getMinStockLevel() : BigDecimal.ZERO) <= 0 ? "CRITICAL" : "HIGH")
                    .status("OPEN")
                    .createdAt(LocalDateTime.now())
                    .build();
            predictiveAlertRepository.save(alert);
            log.warn("[Dispatcher] LOW_STOCK alert raised for reagent {} (available={})", reagent.getReagentCode(), totalAvailable);
        }

        // Create an AUTO_REORDER scheduled task (deduplicated)
        boolean reorderTaskExists = scheduledTaskRepository
                .findByRefEntityAndRefId("InventoryReagent", reagent.getId())
                .stream()
                .anyMatch(t -> t.getTaskType() == TaskType.REAGENT_EXPIRY_CHECK
                        && t.getStatus() == TaskStatusEnum.PENDING);

        if (!reorderTaskExists) {
            ScheduledTask reorderTask = ScheduledTask.builder()
                    .tenantId(tenantId)
                    .branchId(branchId)
                    .taskType(TaskType.REAGENT_EXPIRY_CHECK)
                    .title("Reorder required: " + reagent.getName())
                    .description("Stock for '" + reagent.getName() + "' (" + reagent.getReagentCode()
                            + ") has fallen to " + totalAvailable + " " + reagent.getDefaultUom()
                            + " which is at or below the reorder level of " + reagent.getReorderLevel() + ".")
                    .refEntity("InventoryReagent")
                    .refId(reagent.getId())
                    .dueDate(LocalDate.now())
                    .status(TaskStatusEnum.PENDING)
                    .build();
            scheduledTaskRepository.save(reorderTask);
            log.info("[Dispatcher] AUTO_REORDER task created for reagent {}", reagent.getReagentCode());
        }
    }

    // ── Batch: Expire overdue lots ───────────────────────────────────────────────

    /**
     * Called by SchedulerService daily.
     * Marks all AVAILABLE lots whose expiryDate has passed as EXPIRED,
     * and raises a REAGENT_EXPIRED PredictiveAlert for each.
     */
    @Transactional
    public int expireOverdueLots(Long tenantId) {
        List<InventoryReagentLot> expiredLots = lotRepository
                .findExpiringLots(tenantId, LocalDate.now())
                .stream()
                .filter(l -> l.getExpiryDate() != null && l.getExpiryDate().isBefore(LocalDate.now()))
                .filter(l -> "AVAILABLE".equals(l.getStatus()))
                .toList();

        for (InventoryReagentLot lot : expiredLots) {
            lot.setStatus("EXPIRED");
            lotRepository.save(lot);

            boolean alertExists = predictiveAlertRepository.existsByEntityTypeAndEntityIdAndStatus(
                    "InventoryReagentLot", lot.getId(), "OPEN");
            if (!alertExists) {
                InventoryReagent reagent = lot.getReagent();
                PredictiveAlert alert = PredictiveAlert.builder()
                        .tenantId(tenantId)
                        .branchId(reagent.getBranchId())
                        .alertType("REAGENT_EXPIRED")
                        .entityType("InventoryReagentLot")
                        .entityId(lot.getId())
                        .message("Lot '" + lot.getLotNumber() + "' of reagent '"
                                + reagent.getName() + "' expired on " + lot.getExpiryDate()
                                + ". Remaining qty: " + lot.getCurrentQty() + " " + lot.getUom())
                        .severity("HIGH")
                        .status("OPEN")
                        .createdAt(LocalDateTime.now())
                        .build();
                predictiveAlertRepository.save(alert);
            }
        }

        if (!expiredLots.isEmpty()) {
            log.info("[Dispatcher] Expired {} reagent lots for tenant {}", expiredLots.size(), tenantId);
        }
        return expiredLots.size();
    }

    // ── Batch: Near-expiry alerts ────────────────────────────────────────────────

    /**
     * Called by SchedulerService daily.
     * Raises REAGENT_NEAR_EXPIRY alerts for lots expiring within the next 30 days.
     */
    @Transactional
    public int raiseNearExpiryAlerts(Long tenantId) {
        List<InventoryReagentLot> nearExpiry = lotRepository
                .findExpiringLots(tenantId, LocalDate.now().plusDays(30))
                .stream()
                .filter(l -> l.getExpiryDate() != null && !l.getExpiryDate().isBefore(LocalDate.now()))
                .filter(l -> "AVAILABLE".equals(l.getStatus()))
                .toList();

        int count = 0;
        for (InventoryReagentLot lot : nearExpiry) {
            boolean alertExists = predictiveAlertRepository.existsByEntityTypeAndEntityIdAndStatus(
                    "InventoryReagentLot", lot.getId(), "OPEN");
            if (!alertExists) {
                InventoryReagent reagent = lot.getReagent();
                long daysLeft = LocalDate.now().until(lot.getExpiryDate()).getDays();
                PredictiveAlert alert = PredictiveAlert.builder()
                        .tenantId(tenantId)
                        .branchId(reagent.getBranchId())
                        .alertType("REAGENT_NEAR_EXPIRY")
                        .entityType("InventoryReagentLot")
                        .entityId(lot.getId())
                        .message("Lot '" + lot.getLotNumber() + "' of reagent '"
                                + reagent.getName() + "' expires in " + daysLeft + " day(s) on "
                                + lot.getExpiryDate() + ". Remaining qty: "
                                + lot.getCurrentQty() + " " + lot.getUom())
                        .severity(daysLeft <= 7 ? "CRITICAL" : "MEDIUM")
                        .status("OPEN")
                        .createdAt(LocalDateTime.now())
                        .build();
                predictiveAlertRepository.save(alert);
                count++;
            }
        }

        if (count > 0) log.info("[Dispatcher] Raised {} near-expiry alerts for tenant {}", count, tenantId);
        return count;
    }

    // ── Batch: Stock-level check for all reagents ────────────────────────────────

    /**
     * Called by SchedulerService daily.
     * Checks every ACTIVE reagent in a tenant for stock below reorder level.
     */
    @Transactional
    public int checkAllReorderThresholds(Long tenantId) {
        List<InventoryReagent> reagents = reagentRepository.findByTenantId(tenantId)
                .stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()) && r.getReorderLevel() != null)
                .toList();

        int count = 0;
        for (InventoryReagent reagent : reagents) {
            BigDecimal total = lotRepository.findAvailableByReagentFEFO(reagent.getId())
                    .stream()
                    .map(InventoryReagentLot::getCurrentQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (total.compareTo(reagent.getReorderLevel()) <= 0) {
                boolean alertOpen = predictiveAlertRepository.existsByEntityTypeAndEntityIdAndStatus(
                        "InventoryReagent", reagent.getId(), "OPEN");
                if (!alertOpen) {
                    PredictiveAlert alert = PredictiveAlert.builder()
                            .tenantId(tenantId)
                            .branchId(reagent.getBranchId())
                            .alertType("LOW_STOCK")
                            .entityType("InventoryReagent")
                            .entityId(reagent.getId())
                            .message("Reagent '" + reagent.getName() + "' (" + reagent.getReagentCode()
                                    + ") is below reorder level. Stock: " + total
                                    + " / Reorder at: " + reagent.getReorderLevel())
                            .severity(reagent.getMinStockLevel() != null
                                    && total.compareTo(reagent.getMinStockLevel()) <= 0 ? "CRITICAL" : "HIGH")
                            .status("OPEN")
                            .createdAt(LocalDateTime.now())
                            .build();
                    predictiveAlertRepository.save(alert);
                    count++;
                }
            }
        }

        if (count > 0) log.info("[Dispatcher] Raised {} low-stock alerts for tenant {}", count, tenantId);
        return count;
    }
}
