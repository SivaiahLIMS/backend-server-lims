package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.storage.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.StorageService;
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
@RequestMapping("/storage")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Storage", description = "Storage location and container placement management")
public class StorageController {

    private final StorageService storageService;

    // ── Locations ─────────────────────────────────────────────────────────────

    @GetMapping("/locations")
    @PreAuthorize("hasAuthority('STORAGE_VIEW')")
    @Operation(summary = "List all storage locations for branch",
               description = "Requires: STORAGE_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<StorageLocation>> getLocations(
            @RequestHeader("X-Branch-Id") Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(storageService.getLocations(u.getTenantId(), branchId));
    }

    @GetMapping("/locations/{id}")
    @PreAuthorize("hasAuthority('STORAGE_VIEW')")
    @Operation(summary = "Get storage location by ID",
               description = "Requires: STORAGE_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<StorageLocation> getLocation(@PathVariable Long id) {
        return ResponseEntity.ok(storageService.getLocation(id));
    }

    @PostMapping("/locations")
    @PreAuthorize("hasAuthority('STORAGE_MANAGE')")
    @Operation(summary = "Create storage location",
               description = "Requires: STORAGE_MANAGE. tenantId and branchId are set from JWT and X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<StorageLocation> createLocation(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody StorageLocation location,
            @AuthenticationPrincipal LimsUserDetails u) {
        location.setTenantId(u.getTenantId());
        location.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(storageService.createLocation(location));
    }

    @PutMapping("/locations/{id}")
    @PreAuthorize("hasAuthority('STORAGE_MANAGE')")
    @Operation(summary = "Update storage location",
               description = "Requires: STORAGE_MANAGE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<StorageLocation> updateLocation(
            @PathVariable Long id,
            @RequestBody StorageLocation location) {
        return ResponseEntity.ok(storageService.updateLocation(id, location));
    }

    // ── Container Placement ───────────────────────────────────────────────────

    @PostMapping("/containers/{containerId}/place")
    @PreAuthorize("hasAuthority('STORAGE_MANAGE')")
    @Operation(summary = "Place container in a storage location",
               description = "Requires: STORAGE_MANAGE")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Placed"),
        @ApiResponse(responseCode = "400", description = "Missing locationId"),
        @ApiResponse(responseCode = "404", description = "Container not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ContainerStorage> placeContainer(
            @PathVariable Long containerId,
            @RequestBody PlaceContainerRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                storageService.placeContainer(containerId, body.getLocationId(), body.getUserId())
        );
    }

    @PostMapping("/containers/{containerId}/move")
    @PreAuthorize("hasAuthority('STORAGE_MANAGE')")
    @Operation(summary = "Move container to a different location",
               description = "Requires: STORAGE_MANAGE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Moved"),
        @ApiResponse(responseCode = "400", description = "Missing locationId"),
        @ApiResponse(responseCode = "404", description = "Container not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ContainerStorage> moveContainer(
            @PathVariable Long containerId,
            @RequestBody MoveContainerRequest body) {
        return ResponseEntity.ok(
                storageService.moveContainer(containerId, body.getLocationId(), body.getUserId(), body.getReason())
        );
    }

    @GetMapping("/containers/{containerId}/history")
    @PreAuthorize("hasAuthority('STORAGE_VIEW')")
    @Operation(summary = "Get movement history for a container",
               description = "Requires: STORAGE_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Container not found")
    })
    public ResponseEntity<List<ContainerStorageHistory>> getContainerHistory(@PathVariable Long containerId) {
        return ResponseEntity.ok(storageService.getContainerHistory(containerId));
    }

    // ── Violations ────────────────────────────────────────────────────────────

    @GetMapping("/violations")
    @PreAuthorize("hasAuthority('STORAGE_VIEW')")
    @Operation(summary = "List storage violations for branch",
               description = "Requires: STORAGE_VIEW. Use openOnly=true to filter to unresolved violations.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<StorageViolation>> getViolations(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false, defaultValue = "false") boolean openOnly,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<StorageViolation> result = openOnly
                ? storageService.getOpenViolations(u.getTenantId(), branchId)
                : storageService.getViolations(u.getTenantId(), branchId);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/violations")
    @PreAuthorize("hasAuthority('STORAGE_MANAGE')")
    @Operation(summary = "Create storage violation",
               description = "Requires: STORAGE_MANAGE. tenantId and branchId are set from JWT and X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<StorageViolation> createViolation(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody StorageViolation violation,
            @AuthenticationPrincipal LimsUserDetails u) {
        violation.setTenantId(u.getTenantId());
        violation.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(storageService.createViolation(violation));
    }

    @PostMapping("/violations/{id}/resolve")
    @PreAuthorize("hasAuthority('STORAGE_MANAGE')")
    @Operation(summary = "Resolve a storage violation",
               description = "Requires: STORAGE_MANAGE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resolved"),
        @ApiResponse(responseCode = "404", description = "Violation not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<StorageViolation> resolveViolation(
            @PathVariable Long id,
            @RequestBody ResolveViolationRequest body) {
        return ResponseEntity.ok(storageService.resolveViolation(id, body.getUserId()));
    }
}
