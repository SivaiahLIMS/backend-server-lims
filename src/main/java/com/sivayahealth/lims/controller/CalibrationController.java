package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calibrations")
@RequiredArgsConstructor
@Tag(name = "Calibrations", description = "Unified calibration task management")
public class CalibrationController {

    private final CalibrationTaskRepository calibrationTaskRepository;
    private final InstrumentReadingRepository instrumentReadingRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final InstrumentCalibrationLimitSetRepository calibrationLimitSetRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @Operation(summary = "List calibration tasks")
    public List<CalibrationTask> getCalibrations(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String status) {
        return status != null
                ? calibrationTaskRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status)
                : calibrationTaskRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get calibration task by ID")
    public CalibrationTask getCalibration(@PathVariable Long id) {
        return calibrationTaskRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Calibration task not found: " + id));
    }

    @PostMapping
    @Operation(summary = "Create a calibration task")
    public ResponseEntity<CalibrationTask> createCalibration(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, Object> body) {
        Long instrumentId = Long.valueOf(body.get("instrumentId").toString());
        Long createdById = body.containsKey("createdById") ? Long.valueOf(body.get("createdById").toString()) : null;
        Long limitSetId = body.containsKey("limitSetId") ? Long.valueOf(body.get("limitSetId").toString()) : null;

        InstrumentMaster instrument = instrumentMasterRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found: " + instrumentId));
        AppUser createdBy = createdById != null ? appUserRepository.findById(createdById).orElse(null) : null;
        InstrumentCalibrationLimitSet limitSet = limitSetId != null
                ? calibrationLimitSetRepository.findById(limitSetId).orElse(null) : null;

        CalibrationTask task = CalibrationTask.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .instrument(instrument)
                .status("CREATED")
                .limitSet(limitSet)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();

        if (body.containsKey("scheduledAt")) {
            task.setScheduledAt(LocalDateTime.parse(body.get("scheduledAt").toString()));
        }

        return ResponseEntity.status(201).body(calibrationTaskRepository.save(task));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete a calibration task with readings")
    public ResponseEntity<CalibrationTask> completeCalibration(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, Object> body) {
        CalibrationTask task = calibrationTaskRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Calibration task not found: " + id));

        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : null;
        String readingJson = body.containsKey("readingJson") ? body.get("readingJson").toString() : "{}";
        AppUser user = userId != null ? appUserRepository.findById(userId).orElse(null) : null;

        InstrumentReading reading = InstrumentReading.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .instrument(task.getInstrument())
                .calibrationTask(task)
                .mode("MANUAL")
                .readingJson(readingJson)
                .createdBy(user)
                .createdAt(LocalDateTime.now())
                .build();
        instrumentReadingRepository.save(reading);

        task.setStatus("COMPLETED");
        task.setCompletedAt(LocalDateTime.now());
        return ResponseEntity.ok(calibrationTaskRepository.save(task));
    }
}
