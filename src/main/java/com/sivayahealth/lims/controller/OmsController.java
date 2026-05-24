package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.OmsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/oms")
@RequiredArgsConstructor
@Tag(name = "Order Management System", description = "Purchase orders and goods receipts")
public class OmsController {

    private final OmsService omsService;

    @GetMapping("/orders")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    @Operation(summary = "Get purchase orders")
    public ResponseEntity<List<PurchaseOrder>> getOrders(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(omsService.getPurchaseOrders(u.getTenantId(), branchId));
    }

    @PostMapping("/orders")
    @PreAuthorize("hasAuthority('PO_CREATE')")
    @Operation(summary = "Create a purchase order")
    public ResponseEntity<PurchaseOrder> createOrder(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                omsService.createPurchaseOrder(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        ((Number) body.get("supplierId")).longValue(),
                        u.getUser().getId(),
                        (String) body.get("poNo")
                )
        );
    }

    @PostMapping("/orders/{poId}/approve")
    @PreAuthorize("hasAuthority('PO_APPROVE')")
    @Operation(summary = "Approve a purchase order")
    public ResponseEntity<PurchaseOrder> approveOrder(@PathVariable Long poId,
                                                       @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(omsService.approvePurchaseOrder(poId, u.getUser().getId()));
    }

    @GetMapping("/grn")
    @PreAuthorize("hasAuthority('GRN_VIEW')")
    @Operation(summary = "Get goods receipts")
    public ResponseEntity<List<GoodsReceipt>> getGrns(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(omsService.getGoodsReceipts(u.getTenantId(), branchId));
    }

    @PostMapping("/grn")
    @PreAuthorize("hasAuthority('GRN_CREATE')")
    @Operation(summary = "Create a goods receipt note")
    public ResponseEntity<GoodsReceipt> createGrn(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                omsService.createGrn(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        ((Number) body.get("poId")).longValue(),
                        (String) body.get("grnNo"),
                        u.getUser().getId()
                )
        );
    }
}
