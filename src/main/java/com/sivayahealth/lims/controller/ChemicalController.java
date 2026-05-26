package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.chemical.BranchChemicalAvailability;
import com.sivayahealth.lims.dto.chemical.ChemicalLabelDto;
import com.sivayahealth.lims.dto.chemical.ChemicalSearchResult;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ChemicalService;
import io.swagger.v3.oas.annotations.Operation;
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
import java.util.Map;

@RestController
@RequestMapping("/chemicals")
@RequiredArgsConstructor
@Tag(name = "Chemical Module", description = "Chemical master, registration, stock, issuance, destruction")
public class ChemicalController {

    private final ChemicalService chemicalService;

    @GetMapping("/masters")
    @PreAuthorize("hasAuthority('CHEMICAL_MASTER_VIEW')")
    @Operation(summary = "Get all chemical masters for tenant")
    public ResponseEntity<List<ChemicalMaster>> getMasters(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getChemicalMasters(u.getTenantId()));
    }

    @PostMapping("/masters")
    @PreAuthorize("hasAuthority('CHEMICAL_MASTER_CREATE')")
    @Operation(summary = "Create a chemical master")
    public ResponseEntity<ChemicalMaster> createMaster(@RequestBody ChemicalMaster master,
                                                       @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chemicalService.createChemicalMaster(u.getTenantId(), master));
    }

    @PostMapping("/registrations")
    @PreAuthorize("hasAuthority('CHEMICAL_REGISTER')")
    @Operation(summary = "Register a chemical batch")
    public ResponseEntity<ChemicalRegistration> register(@RequestBody ChemicalRegistration registration,
                                                         @RequestParam Long branchId,
                                                         @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chemicalService.registerChemical(u.getTenantId(), branchId, registration, u.getUser().getId()));
    }

    @GetMapping("/stock")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Get chemical stock for branch")
    public ResponseEntity<List<ChemicalStock>> getStock(@RequestParam Long branchId,
                                                        @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getStockByBranch(u.getTenantId(), branchId));
    }

    @PostMapping("/{registrationId}/issue")
    @PreAuthorize("hasAuthority('CHEMICAL_ISSUE')")
    @Operation(summary = "Issue chemical from stock")
    public ResponseEntity<ChemicalIssuance> issue(@PathVariable Long registrationId,
                                                  @RequestBody Map<String, Object> body,
                                                  @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chemicalService.issueChemical(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        registrationId,
                        new BigDecimal(body.get("quantity").toString()),
                        ((Number) body.get("containers")).intValue(),
                        ((Number) body.get("issuedToId")).longValue(),
                        u.getUser().getId(),
                        (String) body.get("purpose")
                )
        );
    }

    @PostMapping("/{registrationId}/destroy")
    @PreAuthorize("hasAuthority('CHEMICAL_DESTROY')")
    @Operation(summary = "Destroy chemical stock")
    public ResponseEntity<ChemicalDestruction> destroy(@PathVariable Long registrationId,
                                                       @RequestBody Map<String, Object> body,
                                                       @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                chemicalService.destroyChemical(
                        u.getTenantId(),
                        registrationId,
                        new BigDecimal(body.get("quantity").toString()),
                        ((Number) body.get("containers")).intValue(),
                        u.getUser().getId(),
                        body.containsKey("witnessedById") ? ((Number) body.get("witnessedById")).longValue() : null,
                        (String) body.get("method"),
                        (String) body.get("remarks")
                )
        );
    }

    @GetMapping("/expiry-alerts")
    @PreAuthorize("hasAuthority('CHEMICAL_EXPIRY_ALERT_VIEW')")
    @Operation(summary = "Get expiring chemicals")
    public ResponseEntity<List<ChemicalRegistration>> getExpiryAlerts(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getExpiringChemicals(u.getTenantId(), daysAhead));
    }

    @GetMapping("/registrations/{registrationId}/qr")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Download QR code PNG for a chemical bottle")
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
    public ResponseEntity<ChemicalLabelDto> getLabel(@PathVariable Long registrationId,
                                                     @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(chemicalService.getRegistrationLabel(u.getTenantId(), registrationId));
    }

    @PostMapping("/registrations/labels/batch")
    @PreAuthorize("hasAuthority('CHEMICAL_STOCK_VIEW')")
    @Operation(summary = "Get label slips for multiple chemical registrations")
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
    public ResponseEntity<BranchChemicalAvailability> getAvailableExpiringSoon(
            @PathVariable Long branchId,
            @RequestParam(defaultValue = "0") BigDecimal minVolume,
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                chemicalService.getAvailableInBranchExpiringSoon(u.getTenantId(), branchId, minVolume, daysAhead));
    }
}
