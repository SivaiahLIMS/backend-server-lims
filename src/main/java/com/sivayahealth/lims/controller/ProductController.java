package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.product.AddIngredientRequest;
import com.sivayahealth.lims.dto.product.AddProductAttachmentRequest;
import com.sivayahealth.lims.dto.product.ProductCommentRequest;
import com.sivayahealth.lims.dto.product.UpdateProductRequest;
import com.sivayahealth.lims.dto.product.UpsertProductSpecRequest;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ProductService;
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

import java.util.HashMap;
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
               description = "Required: productCode, productName, productType. " +
                             "Optional: strength, dosageForm, batchSize, batchUom, hsnCode, " +
                             "therapeuticCategory, regulatoryStatus, shelfLifeValue, shelfLifeUnit, " +
                             "storageCondition, manufacturerId, siteId, productionLineId, " +
                             "primaryPackaging, secondaryPackaging, samplingPlan, " +
                             "sampleQuantity, sampleUom, qcReviewerId, qcManagerId.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Invalid request or duplicate productCode"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductMaster> create(
            @RequestBody ProductMaster body,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        body.setBranch(null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productService.create(u.getTenantId(), branchId, u.getUser().getId(), body));
    }

    @PutMapping("/{productId}")
    @PreAuthorize("hasAuthority('PRODUCT_EDIT')")
    @Operation(summary = "Update product fields (only DRAFT or REJECTED products)",
               description = "Pass only the fields you want to change. All changes are recorded in product_audit.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "400", description = "Product not in editable state"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductMaster> update(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody UpdateProductRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        Map<String, Object> fields = new HashMap<>();
        if (body.getProductCode()         != null) fields.put("productCode",         body.getProductCode());
        if (body.getProductName()         != null) fields.put("productName",         body.getProductName());
        if (body.getProductType()         != null) fields.put("productType",         body.getProductType());
        if (body.getStrength()            != null) fields.put("strength",            body.getStrength());
        if (body.getDosageForm()          != null) fields.put("dosageForm",          body.getDosageForm());
        if (body.getBatchSize()           != null) fields.put("batchSize",           body.getBatchSize());
        if (body.getBatchUom()            != null) fields.put("batchUom",            body.getBatchUom());
        if (body.getHsnCode()             != null) fields.put("hsnCode",             body.getHsnCode());
        if (body.getTherapeuticCategory() != null) fields.put("therapeuticCategory", body.getTherapeuticCategory());
        if (body.getRegulatoryStatus()    != null) fields.put("regulatoryStatus",    body.getRegulatoryStatus());
        if (body.getShelfLifeValue()      != null) fields.put("shelfLifeValue",      body.getShelfLifeValue());
        if (body.getShelfLifeUnit()       != null) fields.put("shelfLifeUnit",       body.getShelfLifeUnit());
        if (body.getStorageCondition()    != null) fields.put("storageCondition",    body.getStorageCondition());
        if (body.getManufacturerId()      != null) fields.put("manufacturerId",      body.getManufacturerId());
        if (body.getSiteId()              != null) fields.put("siteId",              body.getSiteId());
        if (body.getProductionLineId()    != null) fields.put("productionLineId",    body.getProductionLineId());
        if (body.getPrimaryPackaging()    != null) fields.put("primaryPackaging",    body.getPrimaryPackaging());
        if (body.getSecondaryPackaging()  != null) fields.put("secondaryPackaging",  body.getSecondaryPackaging());
        if (body.getSamplingPlan()        != null) fields.put("samplingPlan",        body.getSamplingPlan());
        if (body.getSampleQuantity()      != null) fields.put("sampleQuantity",      body.getSampleQuantity());
        if (body.getSampleUom()           != null) fields.put("sampleUom",           body.getSampleUom());
        if (body.getQcReviewerId()        != null) fields.put("qcReviewerId",        body.getQcReviewerId());
        if (body.getQcManagerId()         != null) fields.put("qcManagerId",         body.getQcManagerId());
        return ResponseEntity.ok(
                productService.update(u.getTenantId(), branchId, productId, u.getUser().getId(), fields));
    }

    // ── Workflow ──────────────────────────────────────────────────────────────

    @PostMapping("/{productId}/submit")
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    @Operation(summary = "Submit product for review (DRAFT → UNDER_REVIEW)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Submitted"),
        @ApiResponse(responseCode = "400", description = "Product not in DRAFT state"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductMaster> submit(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.submit(u.getTenantId(), branchId, productId, u.getUser().getId()));
    }

    @PostMapping("/{productId}/approve")
    @PreAuthorize("hasAuthority('PRODUCT_APPROVE')")
    @Operation(summary = "Approve product (UNDER_REVIEW → APPROVED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Approved"),
        @ApiResponse(responseCode = "400", description = "Product not in UNDER_REVIEW state"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductMaster> approve(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) ProductCommentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.getComments() : null;
        return ResponseEntity.ok(
                productService.approve(u.getTenantId(), branchId, productId, u.getUser().getId(), comments));
    }

    @PostMapping("/{productId}/reject")
    @PreAuthorize("hasAuthority('PRODUCT_APPROVE')")
    @Operation(summary = "Reject product (UNDER_REVIEW → REJECTED)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rejected"),
        @ApiResponse(responseCode = "400", description = "Product not in UNDER_REVIEW state"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductMaster> reject(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody(required = false) ProductCommentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        String comments = body != null ? body.getComments() : null;
        return ResponseEntity.ok(
                productService.reject(u.getTenantId(), branchId, productId, u.getUser().getId(), comments));
    }

    @GetMapping("/{productId}/workflow")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "Workflow lifecycle history for a product")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ingredient added"),
        @ApiResponse(responseCode = "400", description = "Invalid ingredientId or quantity"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductComposition> addIngredient(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody AddIngredientRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                productService.addIngredient(u.getTenantId(), branchId, productId,
                        body.getIngredientId(), body.getQuantity(), body.getUom(), body.getGrade(),
                        u.getUser().getId()));
    }

    @DeleteMapping("/{productId}/composition/{itemId}")
    @PreAuthorize("hasAuthority('PRODUCT_COMPOSITION_EDIT')")
    @Operation(summary = "Remove an ingredient from the BOM")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Removed"),
        @ApiResponse(responseCode = "404", description = "Item not found")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Specification not found")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saved"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductSpecification> upsertSpec(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody UpsertProductSpecRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(productService.upsertSpec(
                u.getTenantId(), branchId, productId, u.getUser().getId(),
                body.getSpecDocumentPath(), body.getTestMethods(),
                body.getReleaseCriteria(), body.getStabilityRequirements()));
    }

    // ── Attachments ───────────────────────────────────────────────────────────

    @GetMapping("/{productId}/attachments")
    @PreAuthorize("hasAuthority('PRODUCT_VIEW')")
    @Operation(summary = "List product attachments (COA template, MSDS, SDS, label, etc.)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Product not found")
    })
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
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Attachment added"),
        @ApiResponse(responseCode = "400", description = "Invalid fileType"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ProductAttachment> addAttachment(
            @PathVariable Long productId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody AddProductAttachmentRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                productService.addAttachment(u.getTenantId(), branchId, productId,
                        body.getFileName(), body.getFileType(), body.getFilePath(),
                        u.getUser().getId()));
    }

    @DeleteMapping("/{productId}/attachments/{attachmentId}")
    @PreAuthorize("hasAuthority('PRODUCT_ATTACH_UPLOAD')")
    @Operation(summary = "Delete a product attachment")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long productId,
            @PathVariable Long attachmentId,
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        productService.deleteAttachment(u.getTenantId(), branchId, productId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
