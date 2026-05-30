package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.OrderRequest;
import com.sivayahealth.lims.entity.OrderRequestHistory;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.OrderRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/order-requests")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Order Request Module",
     description = "Request new chemicals/instruments, track approvals and deliveries. " +
                   "Lifecycle: DRAFT → SUBMITTED → APPROVED → ORDER_PLACED → RECEIVED → CLOSED")
public class OrderRequestController {

    private final OrderRequestService orderRequestService;

    // ── List endpoints ───────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "List all order requests for branch, optionally filtered by status",
               description = "Requires: ORDER_REQUEST_VIEW. Scoped by X-Branch-Id header.")
    public ResponseEntity<List<OrderRequest>> list(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        if (status != null) {
            return ResponseEntity.ok(orderRequestService.getByBranchAndStatus(u.getTenantId(), branchId, status));
        }
        return ResponseEntity.ok(orderRequestService.getAll(u.getTenantId(), branchId));
    }

    @GetMapping("/due-for-delivery")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Items due for delivery — ORDER_PLACED requests with expected delivery within N days",
               description = "Returns all ORDER_PLACED requests (chemical + instrument) whose expectedDeliveryDate " +
                             "is within the next daysAhead days. Default 30 days.")
    public ResponseEntity<List<OrderRequest>> dueForDelivery(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(orderRequestService.getDueForDelivery(u.getTenantId(), daysAhead));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Get order request details")
    public ResponseEntity<OrderRequest> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderRequestService.getById(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Full lifecycle history for an order request")
    public ResponseEntity<List<OrderRequestHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderRequestService.getHistory(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_REQUEST_CREATE')")
    @Operation(summary = "Create a new order request (starts as DRAFT)",
               description = "Requires: ORDER_REQUEST_CREATE. requestType must be CHEMICAL or INSTRUMENT. " +
                             "Provide chemicalId or instrumentId accordingly. Scoped by X-Branch-Id header.")
    public ResponseEntity<OrderRequest> create(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String requestType = (String) body.get("requestType");
        Long chemicalId    = body.containsKey("chemicalId") && body.get("chemicalId") != null
                             ? ((Number) body.get("chemicalId")).longValue() : null;
        Long instrumentId  = body.containsKey("instrumentId") && body.get("instrumentId") != null
                             ? ((Number) body.get("instrumentId")).longValue() : null;
        BigDecimal qty     = new BigDecimal(body.get("quantity").toString());
        Long uomId         = body.containsKey("uomId") && body.get("uomId") != null
                             ? ((Number) body.get("uomId")).longValue() : null;
        String reason      = (String) body.get("reason");
        Long supplierId    = body.containsKey("supplierId") && body.get("supplierId") != null
                             ? ((Number) body.get("supplierId")).longValue() : null;
        LocalDate requiredBy = body.containsKey("requiredByDate") && body.get("requiredByDate") != null
                             ? LocalDate.parse(body.get("requiredByDate").toString()) : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(
                orderRequestService.create(u.getTenantId(), branchId, u.getUser().getId(),
                        requestType, chemicalId, instrumentId, qty, uomId, reason,
                        supplierId, requiredBy));
    }

    // ── Lifecycle transitions ────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_CREATE')")
    @Operation(summary = "Submit for approval (DRAFT → SUBMITTED)")
    public ResponseEntity<OrderRequest> submit(@PathVariable Long id,
                                               @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(orderRequestService.submit(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_APPROVE')")
    @Operation(summary = "Approve the request (SUBMITTED → APPROVED)")
    public ResponseEntity<OrderRequest> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(orderRequestService.approve(id, u.getUser().getId(), comment));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_APPROVE')")
    @Operation(summary = "Reject the request — returns it to DRAFT",
               description = "Requester can revise and re-submit.")
    public ResponseEntity<OrderRequest> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(orderRequestService.reject(id, u.getUser().getId(), comment));
    }

    @PostMapping("/{id}/place-order")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_PLACE')")
    @Operation(summary = "Place the order with a supplier (APPROVED → ORDER_PLACED)",
               description = "Records PO number, supplier, and expected delivery date.")
    public ResponseEntity<OrderRequest> placeOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String poNumber            = (String) body.get("poNumber");
        Long supplierId            = body.containsKey("supplierId") && body.get("supplierId") != null
                                     ? ((Number) body.get("supplierId")).longValue() : null;
        LocalDate expectedDelivery = body.containsKey("expectedDeliveryDate") && body.get("expectedDeliveryDate") != null
                                     ? LocalDate.parse(body.get("expectedDeliveryDate").toString()) : null;
        String notes               = (String) body.getOrDefault("notes", null);

        return ResponseEntity.ok(orderRequestService.placeOrder(
                id, u.getUser().getId(), poNumber, supplierId, expectedDelivery, notes));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_RECEIVE')")
    @Operation(summary = "Mark items as received (ORDER_PLACED → RECEIVED)",
               description = "Records delivered quantity, date, and delivery notes.")
    public ResponseEntity<OrderRequest> receive(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        BigDecimal deliveredQty = body.containsKey("deliveredQuantity")
                ? new BigDecimal(body.get("deliveredQuantity").toString()) : null;
        String deliveryNotes    = (String) body.getOrDefault("deliveryNotes", null);

        return ResponseEntity.ok(orderRequestService.markReceived(
                id, u.getUser().getId(), deliveredQty, deliveryNotes));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_PLACE')")
    @Operation(summary = "Close the order request (RECEIVED → CLOSED)")
    public ResponseEntity<OrderRequest> close(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.get("comment") : null;
        return ResponseEntity.ok(orderRequestService.close(id, u.getUser().getId(), comment));
    }
}
