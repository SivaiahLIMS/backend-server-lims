package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.supplier.AddSupplierDocumentRequest;
import com.sivayahealth.lims.dto.supplier.RateSupplierRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.SupplierService;
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
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Supplier Module", description = "Supplier management, documents, ratings")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    @Operation(summary = "Get all suppliers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<Supplier>> getSuppliers(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(supplierService.getSuppliers(u.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    @Operation(summary = "Create a supplier")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier,
                                                    @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.createSupplier(u.getTenantId(), supplier, u.getUser().getId()));
    }

    @PostMapping("/{supplierId}/rating")
    @PreAuthorize("hasAuthority('SUPPLIER_RATING')")
    @Operation(summary = "Rate a supplier",
               description = "Required: rating (integer 1–5). Optional: remarks.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rating recorded"),
        @ApiResponse(responseCode = "400", description = "Invalid rating value"),
        @ApiResponse(responseCode = "404", description = "Supplier not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<SupplierRating> rateSupplier(
            @PathVariable Long supplierId,
            @RequestBody RateSupplierRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplierService.rateSupplier(supplierId, body.getRating(), body.getRemarks(), u.getUser().getId())
        );
    }

    @PostMapping("/{supplierId}/documents")
    @PreAuthorize("hasAuthority('SUPPLIER_DOCUMENT_UPLOAD')")
    @Operation(summary = "Upload a supplier document",
               description = "Required: docType. Optional: fileId, version, expiryDate (yyyy-MM-dd).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Document recorded"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Supplier not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<SupplierDocument> uploadDocument(
            @PathVariable Long supplierId,
            @RequestBody AddSupplierDocumentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplierService.addDocument(supplierId, body.getDocType(), body.getFileId(),
                        body.getVersion(), body.getExpiryDate(), u.getUser().getId())
        );
    }

    @GetMapping("/{supplierId}/documents")
    @PreAuthorize("hasAuthority('SUPPLIER_DOCUMENT_VIEW')")
    @Operation(summary = "Get supplier documents")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Supplier not found")
    })
    public ResponseEntity<List<SupplierDocument>> getDocuments(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.getDocuments(supplierId));
    }
}
