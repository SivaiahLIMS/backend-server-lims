package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.ScheduledJobLog;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ScheduledTaskService;
import com.sivayahealth.lims.service.SchedulerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Scheduler", description = "Scheduled job management and execution history")
public class SchedulerController {

    private final SchedulerService schedulerService;
    private final ScheduledTaskService scheduledTaskService;

    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get recent scheduled job logs")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK")})
    public ResponseEntity<List<ScheduledJobLog>> recentJobs() {
        return ResponseEntity.ok(schedulerService.getRecentJobLogs());
    }

    @GetMapping("/jobs/{jobName}")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Get execution history for a specific job")
    public ResponseEntity<List<ScheduledJobLog>> jobsByName(@PathVariable String jobName) {
        return ResponseEntity.ok(schedulerService.getJobLogsByName(jobName));
    }

    @PostMapping("/jobs/calibration-tasks/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Manually trigger calibration task generation job")
    public ResponseEntity<Map<String, String>> triggerCalibrationTasks() {
        schedulerService.autoGenerateCalibrationTasks();
        return ResponseEntity.ok(Map.of("status", "triggered", "job", "CALIBRATION_TASK_GENERATION"));
    }

    @PostMapping("/jobs/reagent-expiry/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Manually trigger reagent expiry check job")
    public ResponseEntity<Map<String, String>> triggerReagentExpiry() {
        schedulerService.checkReagentExpiry();
        return ResponseEntity.ok(Map.of("status", "triggered", "job", "REAGENT_EXPIRY_CHECK"));
    }

    @PostMapping("/jobs/chemical-alerts/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Manually trigger chemical expiry alert generation job")
    public ResponseEntity<Map<String, String>> triggerChemicalAlerts() {
        schedulerService.generateChemicalExpiryAlerts();
        return ResponseEntity.ok(Map.of("status", "triggered", "job", "CHEMICAL_EXPIRY_ALERTS"));
    }

    @PostMapping("/jobs/stability-timepoints/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Manually trigger stability timepoint due alert job")
    public ResponseEntity<Map<String, String>> triggerStabilityTimepoints() {
        schedulerService.checkStabilityTimepointsDue();
        return ResponseEntity.ok(Map.of("status", "triggered", "job", "STABILITY_TIMEPOINT_DUE"));
    }

    @PostMapping("/run-due")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Mark all due scheduled tasks as overdue — intended for cron or admin trigger")
    public ResponseEntity<Map<String, Object>> runDueTasks(@AuthenticationPrincipal LimsUserDetails u) {
        int count = scheduledTaskService.runDueTasks(u.getTenantId());
        return ResponseEntity.ok(Map.of("processed", count, "tenantId", u.getTenantId()));
    }

    @PostMapping("/jobs/reagent-lot-expiry/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Manually trigger reagent lot auto-expiry + near-expiry alert job")
    public ResponseEntity<Map<String, String>> triggerReagentLotExpiryHooks() {
        schedulerService.runReagentExpiryHooks();
        return ResponseEntity.ok(Map.of("status", "triggered", "job", "REAGENT_LOT_EXPIRY_HOOKS"));
    }

    @PostMapping("/jobs/reagent-reorder/trigger")
    @PreAuthorize("hasAuthority('ADMIN')")
    @Operation(summary = "Manually trigger reagent reorder threshold check job")
    public ResponseEntity<Map<String, String>> triggerReagentReorderCheck() {
        schedulerService.runReagentReorderCheck();
        return ResponseEntity.ok(Map.of("status", "triggered", "job", "REAGENT_REORDER_CHECK"));
    }
}
