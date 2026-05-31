package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.order.CreateOrderRequest;
import com.sivayahealth.lims.dto.order.OrderCommentRequest;
import com.sivayahealth.lims.dto.order.PlaceOrderRequest;
import com.sivayahealth.lims.dto.order.ReceiveOrderRequest;
import com.sivayahealth.lims.entity.OrderRequest;
import com.sivayahealth.lims.entity.OrderRequestHistory;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.OrderRequestService;
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<OrderRequest>> dueForDelivery(
            @RequestParam(defaultValue = "30") int daysAhead,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(orderRequestService.getDueForDelivery(u.getTenantId(), daysAhead));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Get order request details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Order request not found")
    })
    public ResponseEntity<OrderRequest> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderRequestService.getById(id));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_VIEW')")
    @Operation(summary = "Full lifecycle history for an order request")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Order request not found")
    })
    public ResponseEntity<List<OrderRequestHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(orderRequestService.getHistory(id));
    }

    // ── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('ORDER_REQUEST_CREATE')")
    @Operation(summary = "Create a new order request (starts as DRAFT)",
               description = "Requires: ORDER_REQUEST_CREATE. requestType must be CHEMICAL or INSTRUMENT. " +
                             "Provide chemicalId or instrumentId accordingly. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid requestType or missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderRequest> create(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody CreateOrderRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                orderRequestService.create(u.getTenantId(), branchId, u.getUser().getId(),
                        body.getRequestType(), body.getChemicalId(), body.getInstrumentId(),
                        body.getQuantity(), body.getUomId(), body.getReason(),
                        body.getSupplierId(), body.getRequiredByDate()));
    }

    // ── Lifecycle transitions ────────────────────────────────────────────────

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_CREATE')")
    @Operation(summary = "Submit for approval (DRAFT → SUBMITTED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submitted"),
        @ApiResponse(responseCode = "400", description = "Order request not in DRAFT state"),
        @ApiResponse(responseCode = "404", description = "Order request not found")
    })
    public ResponseEntity<OrderRequest> submit(@PathVariable Long id,
                                               @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(orderRequestService.submit(id, u.getUser().getId()));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_APPROVE')")
    @Operation(summary = "Approve the request (SUBMITTED → APPROVED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "400", description = "Order request not in SUBMITTED state"),
        @ApiResponse(responseCode = "404", description = "Order request not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderRequest> approve(
            @PathVariable Long id,
            @RequestBody(required = false) OrderCommentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.getComment() : null;
        return ResponseEntity.ok(orderRequestService.approve(id, u.getUser().getId(), comment));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_APPROVE')")
    @Operation(summary = "Reject the request — returns it to DRAFT",
               description = "Requester can revise and re-submit.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rejected"),
        @ApiResponse(responseCode = "400", description = "Order request not in SUBMITTED state"),
        @ApiResponse(responseCode = "404", description = "Order request not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderRequest> reject(
            @PathVariable Long id,
            @RequestBody(required = false) OrderCommentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.getComment() : null;
        return ResponseEntity.ok(orderRequestService.reject(id, u.getUser().getId(), comment));
    }

    @PostMapping("/{id}/place-order")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_PLACE')")
    @Operation(summary = "Place the order with a supplier (APPROVED → ORDER_PLACED)",
               description = "Records PO number, supplier, and expected delivery date.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Order placed"),
        @ApiResponse(responseCode = "400", description = "Order request not in APPROVED state"),
        @ApiResponse(responseCode = "404", description = "Order request not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderRequest> placeOrder(
            @PathVariable Long id,
            @RequestBody PlaceOrderRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(orderRequestService.placeOrder(
                id, u.getUser().getId(), body.getPoNumber(),
                body.getSupplierId(), body.getExpectedDeliveryDate(), body.getNotes()));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_RECEIVE')")
    @Operation(summary = "Mark items as received (ORDER_PLACED → RECEIVED)",
               description = "Records delivered quantity, date, and delivery notes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Received"),
        @ApiResponse(responseCode = "400", description = "Order request not in ORDER_PLACED state"),
        @ApiResponse(responseCode = "404", description = "Order request not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<OrderRequest> receive(
            @PathVariable Long id,
            @RequestBody ReceiveOrderRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(orderRequestService.markReceived(
                id, u.getUser().getId(), body.getDeliveredQuantity(), body.getDeliveryNotes()));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('ORDER_REQUEST_PLACE')")
    @Operation(summary = "Close the order request (RECEIVED → CLOSED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Closed"),
        @ApiResponse(responseCode = "400", description = "Order request not in RECEIVED state"),
        @ApiResponse(responseCode = "404", description = "Order request not found")
    })
    public ResponseEntity<OrderRequest> close(
            @PathVariable Long id,
            @RequestBody(required = false) OrderCommentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comment = body != null ? body.getComment() : null;
        return ResponseEntity.ok(orderRequestService.close(id, u.getUser().getId(), comment));
    }
}
