package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.oms.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.OmsService;
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

import java.util.List;

@RestController
@RequestMapping("/oms")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order Management System", description = "Purchase orders and goods receipts")
public class OmsController {

    private final OmsService omsService;

    // ── Purchase Orders ───────────────────────────────────────────────────────

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    @Operation(summary = "Get purchase orders for branch",
               description = "Requires: ORDER_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<PurchaseOrder>> getOrders(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(omsService.getPurchaseOrders(u.getTenantId(), branchId));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('PO_CREATE')")
    @Operation(summary = "Create a purchase order",
               description = "Requires: PO_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PurchaseOrder> createOrder(
            @RequestBody CreatePurchaseOrderRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                omsService.createPurchaseOrder(
                        u.getTenantId(),
                        body.getBranchId(),
                        body.getSupplierId(),
                        u.getUser().getId(),
                        body.getPoNo()
                )
        );
    }

    @PostMapping("/orders/{poId}/approve")
    @PreAuthorize("hasAuthority('PO_APPROVE')")
    @Operation(summary = "Approve a purchase order",
               description = "Requires: PO_APPROVE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "404", description = "Purchase order not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<PurchaseOrder> approveOrder(
            @PathVariable Long poId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(omsService.approvePurchaseOrder(poId, u.getUser().getId()));
    }

    // ── Goods Receipts ────────────────────────────────────────────────────────

    @GetMapping("/grn")
    @PreAuthorize("hasAuthority('GRN_VIEW')")
    @Operation(summary = "Get goods receipts for branch",
               description = "Requires: GRN_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<GoodsReceipt>> getGrns(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(omsService.getGoodsReceipts(u.getTenantId(), branchId));
    }

    @PostMapping("/grn")
    @PreAuthorize("hasAuthority('GRN_CREATE')")
    @Operation(summary = "Create a goods receipt note",
               description = "Requires: GRN_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "404", description = "Purchase order not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<GoodsReceipt> createGrn(
            @RequestBody CreateGrnRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                omsService.createGrn(
                        u.getTenantId(),
                        body.getBranchId(),
                        body.getPoId(),
                        body.getGrnNo(),
                        u.getUser().getId()
                )
        );
    }
}
