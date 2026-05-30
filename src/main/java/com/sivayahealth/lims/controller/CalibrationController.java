package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.calibration.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/calibrations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Calibrations", description = "Unified calibration task management")
public class CalibrationController {

    private final CalibrationTaskRepository calibrationTaskRepository;
    private final InstrumentReadingRepository instrumentReadingRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final InstrumentCalibrationLimitSetRepository calibrationLimitSetRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "List calibration tasks for branch",
               description = "Requires: CALIBRATION_VIEW. Scoped by X-Branch-Id header. Filter by status param.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<CalibrationTask>> getCalibrations(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<CalibrationTask> tasks = status != null
                ? calibrationTaskRepository.findByTenantIdAndBranchIdAndStatus(u.getTenantId(), branchId, status)
                : calibrationTaskRepository.findByTenantIdAndBranchId(u.getTenantId(), branchId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "Get calibration task by ID",
               description = "Requires: CALIBRATION_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<CalibrationTask> getCalibration(@PathVariable Long id) {
        return ResponseEntity.ok(
                calibrationTaskRepository.findById(id)
                        .orElseThrow(() -> LimsException.notFound("Calibration task not found: " + id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CALIBRATION_CREATE')")
    @Operation(summary = "Create a calibration task",
               description = "Requires: CALIBRATION_CREATE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing instrumentId"),
        @ApiResponse(responseCode = "404", description = "Instrument not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CalibrationTask> createCalibration(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateCalibrationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        InstrumentMaster instrument = instrumentMasterRepository.findById(body.getInstrumentId())
                .orElseThrow(() -> LimsException.notFound("Instrument not found: " + body.getInstrumentId()));
        AppUser createdBy = body.getCreatedById() != null
                ? appUserRepository.findById(body.getCreatedById()).orElse(null) : null;
        InstrumentCalibrationLimitSet limitSet = body.getLimitSetId() != null
                ? calibrationLimitSetRepository.findById(body.getLimitSetId()).orElse(null) : null;

        CalibrationTask task = CalibrationTask.builder()
                .tenantId(u.getTenantId())
                .branchId(branchId)
                .instrument(instrument)
                .status("CREATED")
                .limitSet(limitSet)
                .createdBy(createdBy)
                .scheduledAt(body.getScheduledAt())
                .createdAt(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(calibrationTaskRepository.save(task));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('CALIBRATION_COMPLETE')")
    @Operation(summary = "Complete a calibration task with readings",
               description = "Requires: CALIBRATION_COMPLETE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Completed"),
        @ApiResponse(responseCode = "404", description = "Calibration task not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<CalibrationTask> completeCalibration(
            @PathVariable Long id,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CompleteCalibrationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        CalibrationTask task = calibrationTaskRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Calibration task not found: " + id));

        AppUser user = body.getUserId() != null
                ? appUserRepository.findById(body.getUserId()).orElse(null) : null;
        String readingJson = body.getReadingJson() != null ? body.getReadingJson() : "{}";

        InstrumentReading reading = InstrumentReading.builder()
                .tenantId(u.getTenantId())
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
