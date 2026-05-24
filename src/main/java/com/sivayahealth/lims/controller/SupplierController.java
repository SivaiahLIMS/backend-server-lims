package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@Tag(name = "Supplier Module", description = "Supplier management, documents, ratings")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_VIEW')")
    @Operation(summary = "Get all suppliers")
    public ResponseEntity<List<Supplier>> getSuppliers(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(supplierService.getSuppliers(u.getTenantId()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    @Operation(summary = "Create a supplier")
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier,
                                                    @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(supplierService.createSupplier(u.getTenantId(), supplier, u.getUser().getId()));
    }

    @PostMapping("/{supplierId}/rating")
    @PreAuthorize("hasAuthority('SUPPLIER_RATING')")
    @Operation(summary = "Rate a supplier")
    public ResponseEntity<SupplierRating> rateSupplier(
            @PathVariable Long supplierId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplierService.rateSupplier(
                        supplierId,
                        ((Number) body.get("rating")).intValue(),
                        (String) body.get("remarks"),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/{supplierId}/documents")
    @PreAuthorize("hasAuthority('SUPPLIER_DOCUMENT_UPLOAD')")
    @Operation(summary = "Upload a supplier document")
    public ResponseEntity<SupplierDocument> uploadDocument(
            @PathVariable Long supplierId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                supplierService.addDocument(
                        supplierId,
                        (String) body.get("docType"),
                        body.containsKey("fileId") ? ((Number) body.get("fileId")).longValue() : null,
                        (String) body.get("version"),
                        body.containsKey("expiryDate") ? LocalDate.parse((String) body.get("expiryDate")) : null,
                        u.getUser().getId()
                )
        );
    }

    @GetMapping("/{supplierId}/documents")
    @PreAuthorize("hasAuthority('SUPPLIER_DOCUMENT_VIEW')")
    @Operation(summary = "Get supplier documents")
    public ResponseEntity<List<SupplierDocument>> getDocuments(@PathVariable Long supplierId) {
        return ResponseEntity.ok(supplierService.getDocuments(supplierId));
    }
}
