package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.inventory.*;
import com.sivayahealth.lims.entity.InventoryMovement;
import com.sivayahealth.lims.entity.InventoryReagent;
import com.sivayahealth.lims.entity.InventoryReagentLot;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.InventoryReagentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory/reagents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Reagent Inventory", description = "Reagent master data, lot management, FEFO consumption, and movement tracking")
public class InventoryReagentController {

    private final InventoryReagentService inventoryReagentService;

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "Register a new reagent master")
    public ResponseEntity<InventoryReagent> createReagent(
            @RequestBody CreateReagentRequest body,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                inventoryReagentService.createReagent(u.getTenantId(), branchId, body.getName(),
                        body.getCategory(), body.getFormula(), body.getDefaultUom(),
                        body.getMinStockLevel(), body.getReorderLevel(), u.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "List reagents for the tenant/branch")
    public ResponseEntity<List<InventoryReagent>> getReagents(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(inventoryReagentService.getReagents(u.getTenantId(), branchId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get a reagent by ID")
    public ResponseEntity<InventoryReagent> getReagentById(
            @PathVariable Long id,
            @AuthenticationPrincipal LimsUserDetails u) {

        return ResponseEntity.ok(inventoryReagentService.getReagentById(u.getTenantId(), id));
    }

    @PostMapping("/lots")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "Receive a new lot for a reagent")
    public ResponseEntity<InventoryReagentLot> createLot(
            @RequestBody CreateReagentLotRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                inventoryReagentService.createLot(u.getTenantId(), body.getReagentId(), body.getLotNumber(),
                        body.getSupplierLot(), body.getReceivedQty(), body.getUom(), body.getReceivedDate(),
                        body.getExpiryDate(), body.getManufactureDate(), body.getSupplierId(),
                        body.getStorageLocation(), body.getCertificateNo(), u.getUser().getId()));
    }

    @GetMapping("/{id}/lots")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "List all lots for a reagent")
    public ResponseEntity<List<InventoryReagentLot>> getLots(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean fefo,
            @AuthenticationPrincipal LimsUserDetails u) {
        if (fefo) {
            return ResponseEntity.ok(inventoryReagentService.getAvailableLotsFEFO(id));
        }
        return ResponseEntity.ok(inventoryReagentService.getLotsByReagent(id));
    }

    @PostMapping("/lots/{lotId}/consume")
    @PreAuthorize("hasAuthority('INVENTORY_CONSUME')")
    @Operation(summary = "Consume quantity from a lot")
    public ResponseEntity<InventoryMovement> consumeLot(
            @PathVariable Long lotId,
            @RequestBody ConsumeLotRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                inventoryReagentService.consumeLot(u.getTenantId(), lotId, body.getQuantity(), body.getReason(),
                        body.getRefEntity(), body.getRefId(), u.getUser().getId()));
    }

    @PostMapping("/lots/{lotId}/adjust")
    @PreAuthorize("hasAuthority('INVENTORY_MANAGE')")
    @Operation(summary = "Adjust lot quantity (ADD / SUBTRACT / SET)")
    public ResponseEntity<InventoryMovement> adjustLot(
            @PathVariable Long lotId,
            @RequestBody AdjustLotRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                inventoryReagentService.adjustLot(u.getTenantId(), lotId, body.getQuantity(), body.getMovementType(),
                        body.getReason(), u.getUser().getId()));
    }

    @GetMapping("/lots/{lotId}/movements")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get movement history for a lot")
    public ResponseEntity<List<InventoryMovement>> getMovements(
            @PathVariable Long lotId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(inventoryReagentService.getMovements(lotId));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasAuthority('INVENTORY_VIEW')")
    @Operation(summary = "Get lots expiring within the next N days (default 30)")
    public ResponseEntity<List<InventoryReagentLot>> getExpiringLots(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(inventoryReagentService.getExpiringLots(u.getTenantId(), daysAhead));
    }
}
