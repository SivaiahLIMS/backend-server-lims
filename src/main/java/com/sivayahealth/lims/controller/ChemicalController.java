package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ChemicalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
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
}
