package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.instrument.AddCalibrationResultRequest;
import com.sivayahealth.lims.dto.instrument.CreateInstrumentCalibrationRequest;
import com.sivayahealth.lims.dto.instrument.CreateInstrumentReservationRequest;
import com.sivayahealth.lims.dto.instrument.InstrumentRemarksRequest;
import com.sivayahealth.lims.dto.instrument.LogDowntimeRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.InstrumentService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/instruments")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Instrument Module", description = "Instrument master, calibration, maintenance, downtime")
public class InstrumentController {

    private final InstrumentService instrumentService;
    private final InstrumentReservationRepository instrumentReservationRepository;
    private final InstrumentCalibrationLimitSetRepository calibrationLimitSetRepository;
    private final CalibrationTaskRepository calibrationTaskRepository;
    private final InstrumentMasterRepository instrumentMasterRepository;
    private final InstrumentCalibrationRepository instrumentCalibrationRepository;
    private final AppUserRepository appUserRepository;
    private final OrderRequestRepository orderRequestRepository;

    @PostMapping
    @PreAuthorize("hasAuthority('INSTRUMENT_CREATE')")
    @Operation(summary = "Register a new instrument")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentMaster> createInstrument(@RequestBody InstrumentMaster instrument) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instrumentService.createInstrument(instrument));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INSTRUMENT_VIEW')")
    @Operation(summary = "List instruments for branch",
               description = "Requires: INSTRUMENT_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<InstrumentMaster>> getInstruments(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(instrumentService.getInstrumentsByBranch(u.getTenantId(), branchId));
    }

    @PostMapping("/{instrumentId}/calibrations")
    @PreAuthorize("hasAuthority('CALIBRATION_ALLOCATE')")
    @Operation(summary = "Create a calibration event for instrument",
               description = "Requires: CALIBRATION_ALLOCATE. Body: analystId. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Calibration created"),
        @ApiResponse(responseCode = "404", description = "Instrument not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentCalibration> createCalibration(
            @PathVariable Long instrumentId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateInstrumentCalibrationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                instrumentService.createCalibration(instrumentId, u.getTenantId(), branchId, body.getAnalystId())
        );
    }

    @PostMapping("/calibrations/{calibrationId}/results")
    @PreAuthorize("hasAuthority('CALIBRATION_EXECUTE')")
    @Operation(summary = "Add a calibration result",
               description = "Required: templateId, observation.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Result added"),
        @ApiResponse(responseCode = "404", description = "Calibration not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentCalibrationResult> addResult(
            @PathVariable Long calibrationId,
            @RequestBody AddCalibrationResultRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                instrumentService.addCalibrationResult(
                        calibrationId, body.getTemplateId(), body.getObservation(), u.getUser().getId()
                )
        );
    }

    @PostMapping("/calibrations/{calibrationId}/approve")
    @PreAuthorize("hasAuthority('CALIBRATION_APPROVE')")
    @Operation(summary = "Approve a calibration")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Calibration not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentCalibration> approveCalibration(
            @PathVariable Long calibrationId,
            @RequestBody(required = false) InstrumentRemarksRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String remarks = body != null ? body.getRemarks() : null;
        return ResponseEntity.ok(instrumentService.updateCalibrationStatus(
                calibrationId, "APPROVED", u.getUser().getId(), remarks));
    }

    @PostMapping("/calibrations/{calibrationId}/review")
    @PreAuthorize("hasAuthority('CALIBRATION_REVIEW')")
    @Operation(summary = "Submit calibration for review")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submitted for review"),
        @ApiResponse(responseCode = "404", description = "Calibration not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentCalibration> reviewCalibration(
            @PathVariable Long calibrationId,
            @RequestBody(required = false) InstrumentRemarksRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String remarks = body != null ? body.getRemarks() : null;
        return ResponseEntity.ok(instrumentService.updateCalibrationStatus(
                calibrationId, "TEST_COMPLETED", u.getUser().getId(), remarks));
    }

    @PostMapping("/{instrumentId}/downtime")
    @PreAuthorize("hasAuthority('DOWNTIME_LOG')")
    @Operation(summary = "Log instrument downtime",
               description = "Body: reason (required).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Downtime logged"),
        @ApiResponse(responseCode = "404", description = "Instrument not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentDowntime> logDowntime(
            @PathVariable Long instrumentId,
            @RequestBody LogDowntimeRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                instrumentService.logDowntime(instrumentId, LocalDateTime.now(), body.getReason())
        );
    }

    @GetMapping("/{instrumentId}/reservations")
    @PreAuthorize("hasAuthority('INSTRUMENT_VIEW')")
    @Operation(summary = "List reservations for an instrument")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Instrument not found")
    })
    public ResponseEntity<List<InstrumentReservation>> getReservations(@PathVariable Long instrumentId) {
        return ResponseEntity.ok(instrumentReservationRepository.findByInstrument_Id(instrumentId));
    }

    @PostMapping("/{instrumentId}/reservations")
    @PreAuthorize("hasAuthority('INSTRUMENT_VIEW')")
    @Operation(summary = "Request an instrument reservation",
               description = "Requires: INSTRUMENT_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reservation requested"),
        @ApiResponse(responseCode = "404", description = "Instrument not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentReservation> createReservation(
            @PathVariable Long instrumentId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) CreateInstrumentReservationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        InstrumentMaster instrument = instrumentMasterRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found: " + instrumentId));
        InstrumentReservation reservation = InstrumentReservation.builder()
                .tenantId(u.getTenantId())
                .branchId(branchId)
                .instrument(instrument)
                .status("REQUESTED")
                .requestedBy(u.getUser())
                .requestedAt(LocalDateTime.now())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(instrumentReservationRepository.save(reservation));
    }

    @PostMapping("/reservations/{id}/approve")
    @PreAuthorize("hasAuthority('INSTRUMENT_VIEW')")
    @Operation(summary = "Approve an instrument reservation")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentReservation> approveReservation(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {
        InstrumentReservation reservation = instrumentReservationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Reservation not found: " + id));
        reservation.setStatus("APPROVED");
        reservation.setApprovedBy(u.getUser());
        reservation.setApprovedAt(LocalDateTime.now());
        return ResponseEntity.ok(instrumentReservationRepository.save(reservation));
    }

    @GetMapping("/{instrumentId}/limits")
    @PreAuthorize("hasAuthority('INSTRUMENT_VIEW')")
    @Operation(summary = "Get calibration limit sets for an instrument")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Instrument not found")
    })
    public ResponseEntity<List<InstrumentCalibrationLimitSet>> getLimits(@PathVariable Long instrumentId) {
        return ResponseEntity.ok(calibrationLimitSetRepository.findByInstrument_Id(instrumentId));
    }

    @PostMapping("/{instrumentId}/limits")
    @PreAuthorize("hasAuthority('CALIBRATION_APPROVE')")
    @Operation(summary = "Add a calibration limit set for an instrument")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "404", description = "Instrument not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentCalibrationLimitSet> createLimitSet(
            @PathVariable Long instrumentId,
            @RequestBody InstrumentCalibrationLimitSet limitSet,
            @AuthenticationPrincipal LimsUserDetails u) {
        InstrumentMaster instrument = instrumentMasterRepository.findById(instrumentId)
                .orElseThrow(() -> LimsException.notFound("Instrument not found: " + instrumentId));
        calibrationLimitSetRepository.findByInstrument_IdAndActiveTrue(instrumentId).ifPresent(existing -> {
            existing.setActive(false);
            existing.setEffectiveTo(LocalDateTime.now());
            calibrationLimitSetRepository.save(existing);
        });
        limitSet.setInstrument(instrument);
        limitSet.setCreatedBy(u.getUser());
        limitSet.setTenantId(u.getTenantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(calibrationLimitSetRepository.save(limitSet));
    }

    // ── Operational Lists ────────────────────────────────────────────────────

    @GetMapping("/lists/active")
    @PreAuthorize("hasAuthority('INSTRUMENT_VIEW')")
    @Operation(summary = "Active instruments list — status = AVAILABLE",
               description = "Instruments currently usable in the branch.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<InstrumentMaster>> getActiveInstruments(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                instrumentMasterRepository.findByTenantIdAndBranchIdAndStatus(u.getTenantId(), branchId, "AVAILABLE"));
    }

    @GetMapping("/lists/overdue-calibration")
    @PreAuthorize("hasAuthority('CALIBRATION_SCHEDULE_VIEW')")
    @Operation(summary = "Instruments overdue for calibration",
               description = "Calibration due date is in the past and the calibration record is not yet completed/approved.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<InstrumentCalibration>> getOverdueCalibrations(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                instrumentCalibrationRepository.findOverdueForCalibration(
                        u.getTenantId(), branchId, LocalDate.now()));
    }

    @GetMapping("/lists/ready-for-calibration")
    @PreAuthorize("hasAuthority('CALIBRATION_SCHEDULE_VIEW')")
    @Operation(summary = "Instruments ready for calibration",
               description = "Instrument is AVAILABLE and calibration is SCHEDULED within the window (default next 7 days).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<InstrumentCalibration>> getReadyForCalibration(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(defaultValue = "7") int windowDays,
            @AuthenticationPrincipal LimsUserDetails u) {
        LocalDate today = LocalDate.now();
        return ResponseEntity.ok(
                instrumentCalibrationRepository.findReadyForCalibration(
                        u.getTenantId(), branchId, today, today.plusDays(windowDays)));
    }

    @GetMapping("/lists/pending-calibration-approval")
    @PreAuthorize("hasAuthority('CALIBRATION_REVIEW')")
    @Operation(summary = "Calibrations pending QA/QC approval",
               description = "Calibration completed — waiting for reviewer/approver. Status = TEST_COMPLETED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<InstrumentCalibration>> getPendingCalibrationApproval(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                instrumentCalibrationRepository.findByTenant_IdAndBranch_IdAndStatus(
                        u.getTenantId(), branchId, "TEST_COMPLETED"));
    }

    @GetMapping("/{instrumentId}/calibration-lifecycle")
    @PreAuthorize("hasAuthority('CALIBRATION_SCHEDULE_VIEW')")
    @Operation(summary = "Full calibration lifecycle for an instrument",
               description = "All calibration records ordered chronologically: " +
                             "CREATED → ASSIGNED → IN_PROGRESS → COMPLETED → TEST_COMPLETED → APPROVED → ARCHIVED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Instrument not found")
    })
    public ResponseEntity<List<InstrumentCalibration>> getCalibrationLifecycle(
            @PathVariable Long instrumentId) {
        return ResponseEntity.ok(
                instrumentCalibrationRepository.findByInstrumentIdOrderByCreatedAtAsc(instrumentId));
    }

    @GetMapping("/lists/due-for-delivery")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Instruments due for delivery — ORDER_PLACED instrument order requests",
               description = "All INSTRUMENT order requests in ORDER_PLACED status with expected delivery " +
                             "within the next daysAhead days (default 30).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OrderRequest>> getInstrumentsDueForDelivery(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                orderRequestRepository.findDueForDelivery(u.getTenantId(), LocalDate.now().plusDays(daysAhead))
                        .stream()
                        .filter(o -> "INSTRUMENT".equals(o.getRequestType()))
                        .toList());
    }
}
