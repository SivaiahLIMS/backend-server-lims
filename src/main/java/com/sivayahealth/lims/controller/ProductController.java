package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ProductService;
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
import java.util.List;
import java.util.Map;

/**
 * Product Registration API — every endpoint is scoped to tenantId + branchId.
 *
 * branchId is resolved from:
 *   1. Path/query parameter when supplied
 *   2. JWT claim via LimsUserDetails otherwise
 */
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Product Registration",
     description = "Drug product / raw material registration with BOM, specification, attachments and workflow. " +
                   "ALL endpoints require tenant_id + branch_id scope.")
public class ProductController {

    private final ProductService productService;

    // ── List / Search ─────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "List / search products",
               description = "Required: branchId. Optional filters: status, name (partial), productType.")
    public ResponseEntity<List<ProductMaster>> list(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String productType,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.list(u.getTenantId(), branchId, status, name, productType));
    }

    @GetMapping("/{productId}")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Get a single product by ID (tenant + branch scoped)")
    public ResponseEntity<ProductMaster> getById(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.getById(u.getTenantId(), branchId, productId));
    }

    // ── Create / Update ───────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Register a new product (status starts as DRAFT)",
               description = "Required body fields: productCode, productName, productType, branchId. " +
                             "Optional: strength, dosageForm, batchSize, batchUom, hsnCode, " +
                             "therapeuticCategory, regulatoryStatus, shelfLifeValue, shelfLifeUnit, " +
                             "storageCondition, manufacturerId, siteId, productionLineId, " +
                             "primaryPackaging, secondaryPackaging, samplingPlan, " +
                             "sampleQuantity, sampleUom, qcReviewerId, qcManagerId.")
    public ResponseEntity<ProductMaster> create(
            @RequestBody ProductMaster body,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        body.setBranch(null); // resolved in service
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(u.getTenantId(), branchId, u.getUser().getId(), body));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    @Operation(summary = "Update product fields (only DRAFT or REJECTED products)",
               description = "Pass only the fields you want to change. " +
                             "All changes are recorded in product_audit. " +
                             "branchId query param required for tenant+branch scope check.")
    public ResponseEntity<ProductMaster> update(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> fields,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                productService.update(u.getTenantId(), branchId, productId, u.getUser().getId(), fields));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @PostMapping("/{productId}/submit")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Submit product for review (DRAFT → UNDER_REVIEW)")
    public ResponseEntity<ProductMaster> submit(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.submit(u.getTenantId(), branchId, productId, u.getUser().getId()));
    }

    @PostMapping("/{productId}/approve")
    @PreAuthorize("hasAuthority('PRODUCT_APPROVE')")
    @Operation(summary = "Approve product (UNDER_REVIEW → APPROVED)")
    public ResponseEntity<ProductMaster> approve(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.get("comments") : null;
        return ResponseEntity.ok(
                productService.approve(u.getTenantId(), branchId, productId, u.getUser().getId(), comments));
    }

    @PostMapping("/{productId}/reject")
    @PreAuthorize("hasAuthority('PRODUCT_APPROVE')")
    @Operation(summary = "Reject product (UNDER_REVIEW → REJECTED)")
    public ResponseEntity<ProductMaster> reject(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.get("comments") : null;
        return ResponseEntity.ok(
                productService.reject(u.getTenantId(), branchId, productId, u.getUser().getId(), comments));
    }

    @GetMapping("/{productId}/workflow")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Workflow lifecycle history for a product")
    public ResponseEntity<List<ProductWorkflow>> workflowHistory(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                productService.getWorkflowHistory(u.getTenantId(), branchId, productId));
    }

    @GetMapping("/{productId}/audit")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Field-level audit trail for a product")
    public ResponseEntity<List<ProductAudit>> auditTrail(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                productService.getAuditTrail(u.getTenantId(), branchId, productId));
    }

    // ── Composition (BOM) ─────────────────────────────────────────────────────

    @GetMapping("/{productId}/composition")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Get product BOM / ingredient list")
    public ResponseEntity<List<ProductComposition>> getComposition(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.getComposition(u.getTenantId(), branchId, productId));
    }

    @PostMapping("/{productId}/composition")
    @PreAuthorize("hasAuthority('PRODUCT_COMPOSITION_EDIT')")
    @Operation(summary = "Add an ingredient to the product BOM",
               description = "Required: ingredientId (chemical_master.id), quantity. Optional: uom, grade.")
    public ResponseEntity<ProductComposition> addIngredient(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        Long ingredientId = ((Number) body.get("ingredientId")).longValue();
        BigDecimal qty    = new BigDecimal(body.get("quantity").toString());
        String uom        = body.containsKey("uom")   ? (String) body.get("uom")   : null;
        String grade      = body.containsKey("grade") ? (String) body.get("grade") : null;

        return ResponseEntity.status(HttpStatus.CREATED).body(
                productService.addIngredient(u.getTenantId(), branchId, productId,
                        ingredientId, qty, uom, grade, u.getUser().getId()));
    }

    @DeleteMapping("/{productId}/composition/{itemId}")
    @PreAuthorize("hasAuthority('PRODUCT_COMPOSITION_EDIT')")
    @Operation(summary = "Remove an ingredient from the BOM")
    public ResponseEntity<Void> removeIngredient(
            @PathVariable Long productId,
            @PathVariable Long itemId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        productService.removeIngredient(u.getTenantId(), branchId, productId, itemId);
        return ResponseEntity.noContent().build();
    }

    // ── Specification ─────────────────────────────────────────────────────────

    @GetMapping("/{productId}/specification")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Get specification (test methods, release criteria, stability)")
    public ResponseEntity<ProductSpecification> getSpec(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.getSpec(u.getTenantId(), branchId, productId));
    }

    @PutMapping("/{productId}/specification")
    @PreAuthorize("hasAuthority('PRODUCT_SPEC_EDIT')")
    @Operation(summary = "Create or update product specification (upsert)",
               description = "Fields: specDocumentPath, testMethods (JSON array string), " +
                             "releaseCriteria, stabilityRequirements.")
    public ResponseEntity<ProductSpecification> upsertSpec(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.upsertSpec(
                u.getTenantId(), branchId, productId, u.getUser().getId(),
                body.get("specDocumentPath"),
                body.get("testMethods"),
                body.get("releaseCriteria"),
                body.get("stabilityRequirements")));
    }

    // ── Attachments ───────────────────────────────────────────────────────────

    @GetMapping("/{productId}/attachments")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "List product attachments (COA template, MSDS, SDS, label, etc.)")
    public ResponseEntity<List<ProductAttachment>> getAttachments(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.getAttachments(u.getTenantId(), branchId, productId));
    }

    @PostMapping("/{productId}/attachments")
    @PreAuthorize("hasAuthority('PRODUCT_ATTACH_UPLOAD')")
    @Operation(summary = "Register a new file attachment for a product",
               description = "Provide fileName, fileType (COA_TEMPLATE|MSDS|SDS|LABEL|OTHER), " +
                             "filePath (storage path / URL from client upload).")
    public ResponseEntity<ProductAttachment> addAttachment(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                productService.addAttachment(u.getTenantId(), branchId, productId,
                        body.get("fileName"), body.get("fileType"), body.get("filePath"),
                        u.getUser().getId()));
    }

    @DeleteMapping("/{productId}/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('PRODUCT_ATTACH_UPLOAD')")
    @Operation(summary = "Delete a product attachment")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long productId,
            @PathVariable Long attachmentId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        productService.deleteAttachment(u.getTenantId(), branchId, productId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
