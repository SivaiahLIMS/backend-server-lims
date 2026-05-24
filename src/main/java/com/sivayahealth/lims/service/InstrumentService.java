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
public class InstrumentService {

    private final InstrumentMasterRepository instrumentRepository;
    private final InstrumentCalibrationRepository calibrationRepository;
    private final InstrumentCalibrationResultRepository resultRepository;
    private final InstrumentCalibrationScheduleRepository scheduleRepository;
    private final InstrumentCalibrationStatusHistoryRepository historyRepository;
    private final InstrumentMaintenanceRepository maintenanceRepository;
    private final InstrumentDowntimeRepository downtimeRepository;
    private final InstrumentTestTemplateRepository templateRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public InstrumentMaster createInstrument(InstrumentMaster instrument) {
        if (instrumentRepository.existsByInstrumentCode(instrument.getInstrumentCode())) {
            throw LimsException.conflict("Instrument code already exists");
        }
        InstrumentMaster saved = instrumentRepository.save(instrument);
        auditService.log(instrument.getTenant().getId(), null, "InstrumentMaster", saved.getId(), "CREATE", null, saved.getInstrumentCode());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<InstrumentMaster> getInstrumentsByBranch(Long tenantId, Long branchId) {
        return instrumentRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional
    public InstrumentCalibration createCalibration(Long instrumentId, Long tenantId, Long branchId, Long analystId) {
        InstrumentMaster instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));

        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));

        AppUser analyst = analystId != null ? userRepository.findById(analystId).orElse(null) : null;

        InstrumentCalibration calibration = InstrumentCalibration.builder()
                .instrument(instrument)
                .tenant(tenant)
                .branch(branch)
                .scheduled(true)
                .analyst(analyst)
                .status("DUE_FOR_ANALYSIS")
                .build();

        InstrumentCalibration saved = calibrationRepository.save(calibration);
        logCalibrationHistory(saved, "DUE_FOR_ANALYSIS", analystId);
        auditService.log(tenantId, analystId, "InstrumentCalibration", saved.getId(), "CREATE", null, "DUE_FOR_ANALYSIS");
        return saved;
    }

    @Transactional
    public InstrumentCalibrationResult addCalibrationResult(Long calibrationId, Long templateId,
                                                              java.math.BigDecimal observation, Long userId) {
        InstrumentCalibration calibration = calibrationRepository.findById(calibrationId)
                .orElseThrow(() -> LimsException.notFound("Calibration not found"));

        InstrumentTestTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> LimsException.notFound("Test template not found"));

        boolean pass = true;
        if (template.getSpecMin() != null && observation.compareTo(template.getSpecMin()) < 0) pass = false;
        if (template.getSpecMax() != null && observation.compareTo(template.getSpecMax()) > 0) pass = false;

        InstrumentCalibrationResult result = InstrumentCalibrationResult.builder()
                .calibration(calibration)
                .template(template)
                .observation(observation)
                .passFail(pass ? "PASS" : "FAIL")
                .build();

        return resultRepository.save(result);
    }

    @Transactional
    public InstrumentCalibration updateCalibrationStatus(Long calibrationId, String newStatus, Long userId, String remarks) {
        InstrumentCalibration calibration = calibrationRepository.findById(calibrationId)
                .orElseThrow(() -> LimsException.notFound("Calibration not found"));

        String oldStatus = calibration.getStatus();
        calibration.setStatus(newStatus);
        calibration.setRemarks(remarks);

        if ("APPROVED".equals(newStatus)) {
            calibration.setCalibratedOn(java.time.LocalDate.now());
            scheduleRepository.findByInstrumentId(calibration.getInstrument().getId())
                    .ifPresent(schedule -> {
                        schedule.setLastCalibratedOn(java.time.LocalDate.now());
                        schedule.setNextDueDate(java.time.LocalDate.now()
                                .plusMonths(schedule.getFrequencyMonths()));
                        schedule.setStatus("UPCOMING");
                        scheduleRepository.save(schedule);
                    });
        }

        calibrationRepository.save(calibration);
        logCalibrationHistory(calibration, newStatus, userId);
        auditService.log(calibration.getTenant().getId(), userId, "InstrumentCalibration",
                calibrationId, "STATUS_CHANGE", oldStatus, newStatus);
        return calibration;
    }

    @Transactional
    public InstrumentDowntime logDowntime(Long instrumentId, LocalDateTime startTime, String reason) {
        InstrumentMaster instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found"));

        InstrumentDowntime downtime = InstrumentDowntime.builder()
                .instrument(instrument)
                .startTime(startTime)
                .reason(reason)
                .build();
        return downtimeRepository.save(downtime);
    }

    private void logCalibrationHistory(InstrumentCalibration calibration, String status, Long userId) {
        AppUser user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        InstrumentCalibrationStatusHistory history = InstrumentCalibrationStatusHistory.builder()
                .calibration(calibration)
                .status(status)
                .changedBy(user)
                .build();
        historyRepository.save(history);
    }
}
