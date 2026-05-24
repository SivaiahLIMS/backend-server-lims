package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.service.BarcodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/barcode")
@RequiredArgsConstructor
@Tag(name = "Barcode", description = "Barcode scanning for containers, instruments, and locations")
public class BarcodeController {

    private final BarcodeService barcodeService;

    @PostMapping("/scan/container")
    @Operation(summary = "Resolve chemical container by barcode")
    public ChemicalContainer scanContainer(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestBody Map<String, String> body) {
        return barcodeService.scanContainer(tenantId, body.get("barcodeValue"));
    }

    @PostMapping("/scan/instrument")
    @Operation(summary = "Resolve instrument by barcode")
    public InstrumentMaster scanInstrument(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestBody Map<String, String> body) {
        return barcodeService.scanInstrument(tenantId, body.get("barcodeValue"));
    }

    @PostMapping("/scan/location")
    @Operation(summary = "Resolve storage location by barcode/code")
    public StorageLocation scanLocation(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, String> body) {
        return barcodeService.scanLocation(tenantId, branchId, body.get("barcodeValue"));
    }

    @PostMapping("/scan")
    @Operation(summary = "Universal scan — resolves container, instrument, or location")
    public Map<String, Object> scanAny(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, String> body) {
        return barcodeService.scanAny(tenantId, branchId, body.get("barcodeValue"));
    }
}
