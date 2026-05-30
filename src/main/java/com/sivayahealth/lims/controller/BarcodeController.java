package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.BarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/barcode")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Barcode", description = "Barcode scanning for containers, instruments, and locations")
public class BarcodeController {

    private final BarcodeService barcodeService;

    @PostMapping("/scan/container")
    @PreAuthorize("hasAuthority('BARCODE_SCAN')")
    @Operation(summary = "Resolve chemical container by barcode",
               description = "Requires: BARCODE_SCAN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Container not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalContainer> scanContainer(
            @RequestBody ScanRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(barcodeService.scanContainer(u.getTenantId(), body.getBarcodeValue()));
    }

    @PostMapping("/scan/instrument")
    @PreAuthorize("hasAuthority('BARCODE_SCAN')")
    @Operation(summary = "Resolve instrument by barcode",
               description = "Requires: BARCODE_SCAN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Instrument not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<InstrumentMaster> scanInstrument(
            @RequestBody ScanRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(barcodeService.scanInstrument(u.getTenantId(), body.getBarcodeValue()));
    }

    @PostMapping("/scan/location")
    @PreAuthorize("hasAuthority('BARCODE_SCAN')")
    @Operation(summary = "Resolve storage location by barcode/code",
               description = "Requires: BARCODE_SCAN. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Location not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<StorageLocation> scanLocation(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ScanRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(barcodeService.scanLocation(u.getTenantId(), branchId, body.getBarcodeValue()));
    }

    @PostMapping("/scan")
    @PreAuthorize("hasAuthority('BARCODE_SCAN')")
    @Operation(summary = "Universal scan — resolves container, instrument, or location",
               description = "Requires: BARCODE_SCAN. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resolved entity with type discriminator"),
        @ApiResponse(responseCode = "404", description = "No entity found for barcode"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Map<String, Object>> scanAny(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ScanRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(barcodeService.scanAny(u.getTenantId(), branchId, body.getBarcodeValue()));
    }

    @Data
    static class ScanRequest {
        private String barcodeValue;
    }
}
