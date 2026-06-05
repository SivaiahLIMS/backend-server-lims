package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.calibration.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.CalibrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/calibrations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Calibrations", description = "Calibration task management and instrument calibration schedules")
public class CalibrationController {

    private final CalibrationService calibrationService;

    // ── Tasks ─────────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "List calibration tasks for branch")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "401")})
    public ResponseEntity<List<CalibrationTask>> getTasks(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.getTasks(u.getTenantId(), branchId, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "Get calibration task by ID")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    public ResponseEntity<CalibrationTask> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(calibrationService.getTaskById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CALIBRATION_CREATE')")
    @Operation(summary = "Create a calibration task")
    @ApiResponses({@ApiResponse(responseCode = "201"), @ApiResponse(responseCode = "404")})
    public ResponseEntity<CalibrationTask> createTask(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateCalibrationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                calibrationService.createTask(
                        u.getTenantId(), branchId,
                        body.getInstrumentId(), body.getLimitSetId(),
                        body.getCreatedById(), body.getScheduledAt()));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('CALIBRATION_COMPLETE')")
    @Operation(summary = "Complete a calibration task with readings")
    @ApiResponses({@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "404")})
    public ResponseEntity<CalibrationTask> completeTask(
            @PathVariable Long id,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CompleteCalibrationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                calibrationService.completeTask(
                        u.getTenantId(), branchId, id,
                        body.getReadingJson(), body.getUserId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('CALIBRATION_CREATE')")
    @Operation(summary = "Cancel a calibration task")
    public ResponseEntity<CalibrationTask> cancelTask(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.cancelTask(id, u.getUser().getId()));
    }

    // ── Schedules ─────────────────────────────────────────────────────────────

    @GetMapping("/schedules")
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "List all calibration schedules")
    public ResponseEntity<List<InstrumentCalibrationSchedule>> getAllSchedules(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.getAllSchedules());
    }

    @GetMapping("/schedules/instrument/{instrumentId}")
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "Get calibration schedule for a specific instrument")
    public ResponseEntity<InstrumentCalibrationSchedule> getSchedule(
            @PathVariable Long instrumentId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.getScheduleByInstrument(instrumentId));
    }

    @PutMapping("/schedules/instrument/{instrumentId}")
    @PreAuthorize("hasAuthority('CALIBRATION_CREATE')")
    @Operation(summary = "Create or update calibration schedule for an instrument")
    public ResponseEntity<InstrumentCalibrationSchedule> upsertSchedule(
            @PathVariable Long instrumentId,
            @RequestBody UpsertScheduleRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                calibrationService.upsertSchedule(
                        instrumentId,
                        body.getFrequencyMonths(),
                        body.getToleranceDays(),
                        body.getNextDueDate()));
    }

    @PostMapping("/schedules/instrument/{instrumentId}/deactivate")
    @PreAuthorize("hasAuthority('CALIBRATION_CREATE')")
    @Operation(summary = "Deactivate a calibration schedule")
    public ResponseEntity<InstrumentCalibrationSchedule> deactivateSchedule(
            @PathVariable Long instrumentId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.deactivateSchedule(instrumentId));
    }

    @GetMapping("/schedules/overdue")
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "Get instruments with overdue calibration (past due date)")
    public ResponseEntity<List<InstrumentCalibrationSchedule>> getOverdue(
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.getOverdueSchedules());
    }

    @GetMapping("/schedules/due-soon")
    @PreAuthorize("hasAuthority('CALIBRATION_VIEW')")
    @Operation(summary = "Get instruments with calibration due within N days (default 30)")
    public ResponseEntity<List<InstrumentCalibrationSchedule>> getDueSoon(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(calibrationService.getDueWithinDays(daysAhead));
    }

    @Data
    static class UpsertScheduleRequest {
        private int frequencyMonths;
        private int toleranceDays;
        private LocalDate nextDueDate;
    }
}
