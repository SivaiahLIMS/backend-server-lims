package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalibrationService {

    private final CalibrationTaskRepository calibrationTaskRepository;
    private final InstrumentCalibrationScheduleRepository scheduleRepository;
    private final InstrumentReadingRepository instrumentReadingRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final InstrumentCalibrationLimitSetRepository limitSetRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    // ── Tasks ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<CalibrationTask> getTasks(Long tenantId, Long branchId, String status) {
        if (status != null) {
            return calibrationTaskRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status);
        }
        return calibrationTaskRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public CalibrationTask getTaskById(Long id) {
        return calibrationTaskRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Calibration task not found"));
    }

    @Transactional
    public CalibrationTask createTask(Long tenantId, Long branchId, Long instrumentId,
                                       Long limitSetId, Long createdById, LocalDateTime scheduledAt) {
        InstrumentMaster instrument = instrumentMasterRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found"));
        AppUser createdBy = createdById != null ? userRepository.findById(createdById).orElse(null) : null;
        InstrumentCalibrationLimitSet limitSet = limitSetId != null
                ? limitSetRepository.findById(limitSetId).orElse(null) : null;

        CalibrationTask task = CalibrationTask.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .instrument(instrument)
                .status("CREATED")
                .limitSet(limitSet)
                .createdBy(createdBy)
                .scheduledAt(scheduledAt)
                .createdAt(LocalDateTime.now())
                .build();

        CalibrationTask saved = calibrationTaskRepository.save(task);
        auditService.log(tenantId, createdById, "CalibrationTask", saved.getId(), "CREATE", null, instrumentId.toString());
        return saved;
    }

    @Transactional
    public CalibrationTask completeTask(Long tenantId, Long branchId, Long taskId,
                                         String readingJson, Long userId) {
        CalibrationTask task = calibrationTaskRepository.findById(taskId)
                .orElseThrow(() -> LimsException.notFound("Calibration task not found"));

        AppUser user = userId != null ? userRepository.findById(userId).orElse(null) : null;

        InstrumentReading reading = InstrumentReading.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .instrument(task.getInstrument())
                .calibrationTask(task)
                .mode("MANUAL")
                .readingJson(readingJson != null ? readingJson : "{}")
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();
        instrumentReadingRepository.save(reading);

        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        CalibrationTask saved = calibrationTaskRepository.save(task);

        updateScheduleAfterCompletion(task.getInstrument().getId());
        auditService.log(tenantId, userId, "CalibrationTask", taskId, "COMPLETE", "CREATED", "COMPLETED");
        return saved;
    }

    @Transactional
    public CalibrationTask cancelTask(Long taskId, Long userId) {
        CalibrationTask task = calibrationTaskRepository.findById(taskId)
                .orElseThrow(() -> LimsException.notFound("Calibration task not found"));
        task.setStatus("CANCELLED");
        return calibrationTaskRepository.save(task);
    }

    // ── Schedules ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<InstrumentCalibrationSchedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public InstrumentCalibrationSchedule getScheduleByInstrument(Long instrumentId) {
        return scheduleRepository.findByInstrumentId(instrumentId)
                .orElseThrow(() -> LimsException.notFound("No calibration schedule found for instrument"));
    }

    @Transactional
    public InstrumentCalibrationSchedule upsertSchedule(Long instrumentId, int frequencyMonths,
                                                          int toleranceDays, LocalDate nextDueDate) {
        InstrumentMaster instrument = instrumentMasterRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found"));

        InstrumentCalibrationSchedule schedule = scheduleRepository.findByInstrumentId(instrumentId)
                .orElse(InstrumentCalibrationSchedule.builder().instrument(instrument).build());

        schedule.setFrequencyMonths(frequencyMonths);
        schedule.setToleranceDays(toleranceDays);
        schedule.setNextDueDate(nextDueDate);
        schedule.setStatus("ACTIVE");

        return scheduleRepository.save(schedule);
    }

    @Transactional
    public InstrumentCalibrationSchedule deactivateSchedule(Long instrumentId) {
        InstrumentCalibrationSchedule schedule = scheduleRepository.findByInstrumentId(instrumentId)
                .orElseThrow(() -> LimsException.notFound("No calibration schedule for instrument"));
        schedule.setStatus("INACTIVE");
        return scheduleRepository.save(schedule);
    }

    @Transactional(readOnly = true)
    public List<InstrumentCalibrationSchedule> getOverdueSchedules() {
        return scheduleRepository.findByStatus("ACTIVE").stream()
                .filter(s -> s.getNextDueDate() != null && s.getNextDueDate().isBefore(LocalDate.now()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InstrumentCalibrationSchedule> getDueWithinDays(int days) {
        LocalDate cutoff = LocalDate.now().plusDays(days);
        return scheduleRepository.findByStatus("ACTIVE").stream()
                .filter(s -> s.getNextDueDate() != null && !s.getNextDueDate().isAfter(cutoff))
                .toList();
    }

    private void updateScheduleAfterCompletion(Long instrumentId) {
        scheduleRepository.findByInstrumentId(instrumentId).ifPresent(schedule -> {
            LocalDate now = LocalDate.now();
            schedule.setLastCalibratedOn(now);
            if (schedule.getFrequencyMonths() != null) {
                schedule.setNextDueDate(now.plusMonths(schedule.getFrequencyMonths()));
            }
            scheduleRepository.save(schedule);
        });
    }
}
