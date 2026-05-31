package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.chemical.BranchChemicalAvailability;
import com.sivayahealth.lims.dto.chemical.ChemicalLabelDto;
import com.sivayahealth.lims.dto.chemical.ChemicalSearchResult;
import com.sivayahealth.lims.dto.chemical.DestroyChemicalRequest;
import com.sivayahealth.lims.dto.chemical.IssueChemicalRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.repository.OrderRequestRepository;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ChemicalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/chemicals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Chemical Module", description = "Chemical master, registration, stock, issuance, destruction")
public class ChemicalController {

    private final ChemicalService chemicalService;
    private final OrderRequestRepository orderRequestRepository;

    @GetMapping("/masters")
    @PreAuthorize("hasAuthority('CHEMICAL_MASTER_VIEW')")
    @Operation(summary = "Get all chemical masters for tenant")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ChemicalMaster>> getMasters(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getChemicalMasters(u.getTenantId()));
    }

    @PostMapping("/masters")
    @PreAuthorize("hasAuthority('CHEMICAL_MASTER_CREATE')")
    @Operation(summary = "Create a chemical master")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalMaster> createMaster(@RequestBody ChemicalMaster master,
                                                         @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chemicalService.createChemicalMaster(u.getTenantId(), master));
    }

    @PostMapping("/registrations")
    @PreAuthorize("hasAuthority('CHEMICAL_REGISTER')")
    @Operation(summary = "Register a chemical batch")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Registered"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalRegistration> register(@RequestBody ChemicalRegistration registration,
                                                          @RequestHeader("X-Branch-Id") Long branchId,
                                                          @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chemicalService.registerChemical(u.getTenantId(), branchId, registration, u.getUser().getId()));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Get chemical stock for branch")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<ChemicalStock>> getStock(@RequestHeader("X-Branch-Id") Long branchId,
                                                         @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getStockByBranch(u.getTenantId(), branchId));
    }

    @PostMapping("/{registrationId}/issue")
    @PreAuthorize("hasAuthority('CHEMICAL_ISSUE')")
    @Operation(summary = "Issue chemical from stock",
               description = "Required: quantity, containers, issuedToId. Optional: purpose.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Issued"),
        @ApiResponse(responseCode = "400", description = "Insufficient stock or invalid request"),
        @ApiResponse(responseCode = "404", description = "Registration not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalIssuance> issue(@PathVariable Long registrationId,
                                                   @RequestHeader("X-Branch-Id") Long branchId,
                                                   @RequestBody IssueChemicalRequest body,
                                                   @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chemicalService.issueChemical(
                        u.getTenantId(), branchId, registrationId,
                        body.getQuantity(), body.getContainers(),
                        body.getIssuedToId(), u.getUser().getId(), body.getPurpose()
                )
        );
    }

    @PostMapping("/{registrationId}/destroy")
    @PreAuthorize("hasAuthority('CHEMICAL_DESTROY')")
    @Operation(summary = "Destroy chemical stock",
               description = "Required: quantity, containers. Optional: witnessedById, method, remarks.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Destruction recorded"),
        @ApiResponse(responseCode = "400", description = "Insufficient stock or invalid request"),
        @ApiResponse(responseCode = "404", description = "Registration not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalDestruction> destroy(@PathVariable Long registrationId,
                                                        @RequestBody DestroyChemicalRequest body,
                                                        @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chemicalService.destroyChemical(
                        u.getTenantId(), registrationId,
                        body.getQuantity(), body.getContainers(),
                        u.getUser().getId(), body.getWitnessedById(),
                        body.getMethod(), body.getRemarks()
                )
        );
    }

    @GetMapping("/expiry-alerts")
    @PreAuthorize("hasAuthority('CHEMICAL_EXPIRY_ALERT_VIEW')")
    @Operation(summary = "Get expiring chemicals")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChemicalRegistration>> getExpiryAlerts(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getExpiringChemicals(u.getTenantId(), daysAhead));
    }

    @GetMapping("/registrations/{registrationId}/qr")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Download QR code PNG for a chemical bottle")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PNG image"),
        @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<byte[]> getQrCode(@PathVariable Long registrationId,
                                             @AuthenticationPrincipal LimsUserDetails u) {
        byte[] png = chemicalService.getRegistrationQrPng(u.getTenantId(), registrationId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentDispositionFormData("attachment", "qr-" + registrationId + ".png");
        return new ResponseEntity<>(png, headers, HttpStatus.OK);
    }

    @GetMapping("/registrations/{registrationId}/label")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Get label slip data (with QR base64) for a chemical bottle")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Label data"),
        @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<ChemicalLabelDto> getLabel(@PathVariable Long registrationId,
                                                      @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getRegistrationLabel(u.getTenantId(), registrationId));
    }

    @PostMapping("/registrations/labels/batch")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Get label slips for multiple chemical registrations")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Label list"),
        @ApiResponse(responseCode = "400", description = "Empty ID list")
    })
    public ResponseEntity<List<ChemicalLabelDto>> getLabelsBatch(@RequestBody List<Long> registrationIds,
                                                                   @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getRegistrationLabels(u.getTenantId(), registrationIds));
    }

    // ── Search & Availability Queries ────────────────────────────────────────

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Search chemicals by name with a minimum available volume filter",
               description = "Returns chemicals (aggregated across all registrations) whose name matches " +
                             "the query and whose total available stock is >= minVolume. " +
                             "Includes per-registration detail lines.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Missing name parameter")
    })
    public ResponseEntity<List<ChemicalSearchResult>> searchByNameAndVolume(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") BigDecimal minVolume,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.searchByNameAndVolume(u.getTenantId(), name, minVolume));
    }

    @GetMapping("/availability/branch/{branchId}")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Available chemicals in a branch filtered by expiry date range and minimum volume",
               description = "Returns chemicals in the given branch that are AVAILABLE, " +
                             "have expiry date within [expiryFrom, expiryTo], and total stock >= minVolume. " +
                             "Sorted by earliest expiry. Includes per-registration detail.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    public ResponseEntity<BranchChemicalAvailability> getAvailableInBranch(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") BigDecimal minVolume,
            @RequestParam(required = false) LocalDate expiryFrom,
            @RequestParam(required = false) LocalDate expiryTo,
            @AuthenticationPrincipal LimsUserDetails u) {
        LocalDate from = expiryFrom != null ? expiryFrom : LocalDate.now();
        LocalDate to   = expiryTo   != null ? expiryTo   : LocalDate.now().plusYears(10);
        return ResponseEntity.ok(chemicalService.getAvailableInBranch(u.getTenantId(), branchId, minVolume, from, to));
    }

    @GetMapping("/availability/branch/{branchId}/expiring-soon")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Available chemicals in a branch expiring within N days with minimum volume",
               description = "Convenience endpoint: expiry window is [today, today + daysAhead]. " +
                             "Only returns chemicals with stock >= minVolume.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Branch not found")
    })
    public ResponseEntity<BranchChemicalAvailability> getAvailableExpiringSoon(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") BigDecimal minVolume,
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                chemicalService.getAvailableInBranchExpiringSoon(u.getTenantId(), branchId, minVolume, daysAhead));
    }

    // ── Delivery Tracking Lists ──────────────────────────────────────────────

    @GetMapping("/lists/due-for-delivery")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Chemicals due for delivery — ORDER_PLACED chemical order requests",
               description = "All CHEMICAL order requests in ORDER_PLACED status with expected delivery " +
                             "within the next daysAhead days (default 30). " +
                             "Use /order-requests/due-for-delivery for combined chemical+instrument view.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OrderRequest>> getChemicalsDueForDelivery(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                orderRequestRepository.findDueForDelivery(u.getTenantId(), LocalDate.now().plusDays(daysAhead))
                        .stream()
                        .filter(o -> "CHEMICAL".equals(o.getRequestType()))
                        .toList());
    }

    @GetMapping("/lists/available-stock")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Full available stock list — all AVAILABLE chemicals with current quantity and FEFO",
               description = "Returns all chemicals with status=AVAILABLE across the tenant. " +
                             "Results include expiry date for FEFO-based selection. " +
                             "Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<BranchChemicalAvailability> getAvailableStockList(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                chemicalService.getAvailableInBranch(
                        u.getTenantId(), branchId,
                        BigDecimal.ZERO,
                        LocalDate.of(2000, 1, 1),
                        LocalDate.now().plusYears(20)));
    }
}
