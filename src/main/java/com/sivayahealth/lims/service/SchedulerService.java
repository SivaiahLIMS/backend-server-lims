package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final InstrumentCalibrationScheduleRepository calibrationScheduleRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final CalibrationTaskRepository calibrationTaskRepository;
    private final ReagentPreparationRepository reagentPreparationRepository;
    private final ChemicalRegistrationRepository chemicalRegistrationRepository;
    private final PredictiveAlertRepository predictiveAlertRepository;
    private final StabilityStudyTimepointRepository stabilityTimepointRepository;
    private final ScheduledJobLogRepository jobLogRepository;
    private final TenantRepository tenantRepository;
    private final ReagentInventoryDispatcher reagentInventoryDispatcher;

    // ── Calibration task auto-generation: daily at 6am ───────────────────────────

    @Scheduled(cron = "0 0 6 * * ?")
    @Transactional
    public void autoGenerateCalibrationTasks() {
        ScheduledJobLog log = startJob("CALIBRATION_TASK_GENERATION");
        int count = 0;
        try {
            LocalDate today = LocalDate.now();
            List<InstrumentCalibrationSchedule> dueSchedules =
                    calibrationScheduleRepository.findByNextDueDateLessThanEqualAndStatus(today, "ACTIVE");

            for (InstrumentCalibrationSchedule schedule : dueSchedules) {
                boolean taskExists = calibrationTaskRepository
                        .existsByScheduleIdAndStatusIn(schedule.getId(),
                                List.of("PENDING", "IN_PROGRESS"));
                if (!taskExists) {
                    CalibrationTask task = CalibrationTask.builder()
                            .instrument(schedule.getInstrument())
                            .schedule(schedule)
                            .taskType("SCHEDULED")
                            .status("PENDING")
                            .dueDate(schedule.getNextDueDate())
                            .build();
                    calibrationTaskRepository.save(task);
                    schedule.setStatus("DUE");
                    calibrationScheduleRepository.save(schedule);
                    count++;
                }
            }
            completeJob(log, count, "Generated " + count + " calibration task(s)");
        } catch (Exception e) {
            failJob(log, e.getMessage());
        }
    }

    // ── Overdue calibration schedule flagging: daily at 6:30am ──────────────────

    @Scheduled(cron = "0 30 6 * * ?")
    @Transactional
    public void flagOverdueCalibrations() {
        ScheduledJobLog logEntry = startJob("CALIBRATION_OVERDUE_FLAG");
        int count = 0;
        try {
            LocalDate overdueThreshold = LocalDate.now().minusDays(1);
            List<InstrumentCalibrationSchedule> overdueSchedules =
                    calibrationScheduleRepository.findByNextDueDateLessThanAndStatus(overdueThreshold, "DUE");

            for (InstrumentCalibrationSchedule schedule : overdueSchedules) {
                schedule.setStatus("OVERDUE");
                calibrationScheduleRepository.save(schedule);
                count++;
            }
            completeJob(logEntry, count, "Flagged " + count + " overdue calibration schedule(s)");
        } catch (Exception e) {
            failJob(logEntry, e.getMessage());
        }
    }

    // ── Reagent expiry check: daily at 7am ───────────────────────────────────────

    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void checkReagentExpiry() {
        ScheduledJobLog logEntry = startJob("REAGENT_EXPIRY_CHECK");
        int count = 0;
        try {
            LocalDate today = LocalDate.now();
            List<ReagentPreparation> expired =
                    reagentPreparationRepository.findByStatusAndExpiryDateBefore("ACTIVE", today);

            for (ReagentPreparation reagent : expired) {
                reagent.setStatus("EXPIRED");
                reagentPreparationRepository.save(reagent);
                count++;
            }
            completeJob(logEntry, count, "Expired " + count + " reagent preparation(s)");
        } catch (Exception e) {
            failJob(logEntry, e.getMessage());
        }
    }

    // ── Chemical expiry alert generation: daily at 7:30am ───────────────────────

    @Scheduled(cron = "0 30 7 * * ?")
    @Transactional
    public void generateChemicalExpiryAlerts() {
        ScheduledJobLog logEntry = startJob("CHEMICAL_EXPIRY_ALERTS");
        int count = 0;
        try {
            LocalDate alertThreshold = LocalDate.now().plusDays(30);
            List<ChemicalRegistration> expiringSoon =
                    chemicalRegistrationRepository.findExpiringAllTenants(alertThreshold);

            for (ChemicalRegistration chem : expiringSoon) {
                String alertKey = "CHEM_EXPIRY_" + chem.getId();
                boolean alertExists = predictiveAlertRepository
                        .existsByEntityTypeAndEntityIdAndStatus("ChemicalRegistration", chem.getId(), "OPEN");
                if (!alertExists) {
                    PredictiveAlert alert = PredictiveAlert.builder()
                            .tenantId(chem.getTenant().getId())
                            .branchId(chem.getBranch().getId())
                            .alertType("CHEMICAL_EXPIRY")
                            .entityType("ChemicalRegistration")
                            .entityId(chem.getId())
                            .message("Chemical '" + chem.getChemical().getName() + "' (Lot: "
                                    + chem.getLotNo() + ") expires on " + chem.getExpiryDate())
                            .severity("HIGH")
                            .status("OPEN")
                            .createdAt(LocalDateTime.now())
                            .build();
                    predictiveAlertRepository.save(alert);
                    count++;
                }
            }
            completeJob(logEntry, count, "Generated " + count + " chemical expiry alert(s)");
        } catch (Exception e) {
            failJob(logEntry, e.getMessage());
        }
    }

    // ── Stability timepoint due alerts: daily at 8am ─────────────────────────────

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void checkStabilityTimepointsDue() {
        ScheduledJobLog logEntry = startJob("STABILITY_TIMEPOINT_DUE");
        int count = 0;
        try {
            LocalDate dueThreshold = LocalDate.now().plusDays(7);
            List<StabilityStudyTimepoint> dueSoon =
                    stabilityTimepointRepository.findByStatusAndScheduledDateBefore("PENDING", dueThreshold);

            for (StabilityStudyTimepoint tp : dueSoon) {
                boolean alertExists = predictiveAlertRepository
                        .existsByEntityTypeAndEntityIdAndStatus("StabilityStudyTimepoint", tp.getId(), "OPEN");
                if (!alertExists) {
                    PredictiveAlert alert = PredictiveAlert.builder()
                            .tenantId(tp.getStudy().getTenant().getId())
                            .branchId(tp.getStudy().getBranchId())
                            .alertType("STABILITY_TIMEPOINT_DUE")
                            .entityType("StabilityStudyTimepoint")
                            .entityId(tp.getId())
                            .message("Stability timepoint " + tp.getTimepoint()
                                    + " for study " + tp.getStudy().getStudyCode()
                                    + " is due on " + tp.getScheduledDate())
                            .severity("MEDIUM")
                            .status("OPEN")
                            .createdAt(LocalDateTime.now())
                            .build();
                    predictiveAlertRepository.save(alert);
                    count++;
                }
            }
            completeJob(logEntry, count, "Generated " + count + " stability timepoint alert(s)");
        } catch (Exception e) {
            failJob(logEntry, e.getMessage());
        }
    }

    // ── Job log retrieval ────────────────────────────────────────────────────────

    public List<ScheduledJobLog> getRecentJobLogs() {
        return jobLogRepository.findTop20ByOrderByStartedAtDesc();
    }

    public List<ScheduledJobLog> getJobLogsByName(String jobName) {
        return jobLogRepository.findByJobNameOrderByStartedAtDesc(jobName);
    }

    // ── Reagent: auto-expire lots + near-expiry alerts: daily at 7:00am ─────────

    @Scheduled(cron = "0 0 7 * * ?")
    @Transactional
    public void runReagentExpiryHooks() {
        ScheduledJobLog logEntry = startJob("REAGENT_LOT_EXPIRY_HOOKS");
        int expired = 0, alerts = 0;
        try {
            List<Long> tenantIds = tenantRepository.findAll()
                    .stream().map(t -> t.getId()).toList();

            for (Long tenantId : tenantIds) {
                expired += reagentInventoryDispatcher.expireOverdueLots(tenantId);
                alerts  += reagentInventoryDispatcher.raiseNearExpiryAlerts(tenantId);
            }
            completeJob(logEntry, expired + alerts,
                    "Expired " + expired + " lot(s), raised " + alerts + " near-expiry alert(s)");
        } catch (Exception e) {
            failJob(logEntry, e.getMessage());
        }
    }

    // ── Reagent: auto-reorder check: daily at 8:00am ─────────────────────────────

    @Scheduled(cron = "0 0 8 * * ?")
    @Transactional
    public void runReagentReorderCheck() {
        ScheduledJobLog logEntry = startJob("REAGENT_REORDER_CHECK");
        int count = 0;
        try {
            List<Long> tenantIds = tenantRepository.findAll()
                    .stream().map(t -> t.getId()).toList();

            for (Long tenantId : tenantIds) {
                count += reagentInventoryDispatcher.checkAllReorderThresholds(tenantId);
            }
            completeJob(logEntry, count, "Raised " + count + " reorder alert(s)");
        } catch (Exception e) {
            failJob(logEntry, e.getMessage());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private ScheduledJobLog startJob(String jobName) {
        ScheduledJobLog entry = ScheduledJobLog.builder()
                .jobName(jobName)
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build();
        return jobLogRepository.save(entry);
    }

    private void completeJob(ScheduledJobLog entry, int records, String message) {
        entry.setStatus("SUCCESS");
        entry.setCompletedAt(LocalDateTime.now());
        entry.setRecordsProcessed(records);
        entry.setMessage(message);
        jobLogRepository.save(entry);
        log.info("[{}] {}", entry.getJobName(), message);
    }

    private void failJob(ScheduledJobLog entry, String errorMessage) {
        entry.setStatus("FAILED");
        entry.setCompletedAt(LocalDateTime.now());
        entry.setMessage(errorMessage);
        jobLogRepository.save(entry);
        log.error("[{}] Job failed: {}", entry.getJobName(), errorMessage);
    }
}
