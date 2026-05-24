package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Storage location and container placement management")
public class StorageController {

    private final StorageService storageService;

    @GetMapping("/locations")
    @Operation(summary = "List all storage locations")
    public List<StorageLocation> getLocations(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId) {
        return storageService.getLocations(tenantId, branchId);
    }

    @GetMapping("/locations/{id}")
    @Operation(summary = "Get storage location by ID")
    public StorageLocation getLocation(@PathVariable Long id) {
        return storageService.getLocation(id);
    }

    @PostMapping("/locations")
    @Operation(summary = "Create storage location")
    public ResponseEntity<StorageLocation> createLocation(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody StorageLocation location) {
        location.setTenantId(tenantId);
        location.setBranchId(branchId);
        return ResponseEntity.status(201).body(storageService.createLocation(location));
    }

    @PutMapping("/locations/{id}")
    @Operation(summary = "Update storage location")
    public StorageLocation updateLocation(@PathVariable Long id, @RequestBody StorageLocation location) {
        return storageService.updateLocation(id, location);
    }

    @PostMapping("/containers/{containerId}/place")
    @Operation(summary = "Place container in a storage location")
    public ResponseEntity<ContainerStorage> placeContainer(
            @PathVariable Long containerId,
            @RequestBody Map<String, Object> body) {
        Long locationId = Long.valueOf(body.get("locationId").toString());
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : null;
        return ResponseEntity.status(201).body(storageService.placeContainer(containerId, locationId, userId));
    }

    @PostMapping("/containers/{containerId}/move")
    @Operation(summary = "Move container to a different location")
    public ContainerStorage moveContainer(
            @PathVariable Long containerId,
            @RequestBody Map<String, Object> body) {
        Long locationId = Long.valueOf(body.get("locationId").toString());
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : null;
        String reason = body.containsKey("reason") ? body.get("reason").toString() : null;
        return storageService.moveContainer(containerId, locationId, userId, reason);
    }

    @GetMapping("/containers/{containerId}/history")
    @Operation(summary = "Get movement history for a container")
    public List<ContainerStorageHistory> getContainerHistory(@PathVariable Long containerId) {
        return storageService.getContainerHistory(containerId);
    }

    @GetMapping("/violations")
    @Operation(summary = "List storage violations")
    public List<StorageViolation> getViolations(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly) {
        return openOnly
                ? storageService.getOpenViolations(tenantId, branchId)
                : storageService.getViolations(tenantId, branchId);
    }

    @PostMapping("/violations")
    @Operation(summary = "Create storage violation")
    public ResponseEntity<StorageViolation> createViolation(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody StorageViolation violation) {
        violation.setTenantId(tenantId);
        violation.setBranchId(branchId);
        return ResponseEntity.status(201).body(storageService.createViolation(violation));
    }

    @PostMapping("/violations/{id}/resolve")
    @Operation(summary = "Resolve a storage violation")
    public StorageViolation resolveViolation(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : null;
        return storageService.resolveViolation(id, userId);
    }
}
