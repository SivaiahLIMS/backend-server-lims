package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.dto.container.*;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import com.sivayahealth.lims.security.LimsUserDetails;
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

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Containers", description = "Chemical container lifecycle with FEFO support")
public class ContainerController {

    private final ChemicalContainerRepository containerRepository;
    private final ChemicalContainerReservationRepository reservationRepository;
    private final DocumentChemicalConsumptionRepository consumptionRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('CONTAINER_VIEW')")
    @Operation(summary = "List all chemical containers for branch",
               description = "Requires: CONTAINER_VIEW. Scoped by X-Branch-Id header. Filter by status param.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChemicalContainer>> getContainers(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<ChemicalContainer> containers = status != null
                ? containerRepository.findByTenantIdAndBranchIdAndStatus(u.getTenantId(), branchId, status)
                : containerRepository.findByTenantIdAndBranchId(u.getTenantId(), branchId);
        return ResponseEntity.ok(containers);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTAINER_VIEW')")
    @Operation(summary = "Get container by ID",
               description = "Requires: CONTAINER_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ChemicalContainer> getContainer(@PathVariable Long id) {
        return ResponseEntity.ok(
                containerRepository.findById(id)
                        .orElseThrow(() -> LimsException.notFound("Container not found: " + id))
        );
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CONTAINER_MANAGE')")
    @Operation(summary = "Create a new chemical container",
               description = "Requires: CONTAINER_MANAGE. tenantId and branchId set from JWT and X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalContainer> createContainer(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ChemicalContainer container,
            @AuthenticationPrincipal LimsUserDetails u) {
        container.setTenantId(u.getTenantId());
        container.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(containerRepository.save(container));
    }

    @GetMapping("/reservations/fefo-select")
    @PreAuthorize("hasAuthority('CONTAINER_VIEW')")
    @Operation(summary = "FEFO-based container selection for a chemical",
               description = "Requires: CONTAINER_VIEW. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ChemicalContainer>> fefoSelect(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam Long chemicalId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(
                containerRepository.findAvailableByChemicalIdOrderByFEFO(u.getTenantId(), branchId, chemicalId)
        );
    }

    @PostMapping("/reservations")
    @PreAuthorize("hasAuthority('CONTAINER_RESERVE')")
    @Operation(summary = "Reserve a container (FEFO-based)",
               description = "Requires: CONTAINER_RESERVE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Reserved"),
        @ApiResponse(responseCode = "400", description = "Missing containerId or reservedQty"),
        @ApiResponse(responseCode = "404", description = "Container not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ChemicalContainerReservation> reserveContainer(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ReserveContainerRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        ChemicalContainer container = containerRepository.findById(body.getContainerId())
                .orElseThrow(() -> LimsException.notFound("Container not found: " + body.getContainerId()));
        AppUser user = body.getUserId() != null
                ? appUserRepository.findById(body.getUserId()).orElse(null) : null;

        ChemicalContainerReservation reservation = ChemicalContainerReservation.builder()
                .tenantId(u.getTenantId())
                .branchId(branchId)
                .container(container)
                .reservedQty(body.getReservedQty())
                .status("ACTIVE")
                .reservedBy(user)
                .reservedAt(LocalDateTime.now())
                .build();

        container.setStatus("RESERVED");
        containerRepository.save(container);

        return ResponseEntity.status(HttpStatus.CREATED).body(reservationRepository.save(reservation));
    }

    @PostMapping("/reservations/{id}/convert")
    @PreAuthorize("hasAuthority('CONTAINER_RESERVE')")
    @Operation(summary = "Convert reservation to consumption",
               description = "Requires: CONTAINER_RESERVE. Scoped by X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Converted"),
        @ApiResponse(responseCode = "404", description = "Reservation not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<DocumentChemicalConsumption> convertReservation(
            @PathVariable Long id,
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ConvertReservationRequest body,
            @AuthenticationPrincipal LimsUserDetails u) {
        ChemicalContainerReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Reservation not found: " + id));

        java.math.BigDecimal consumedQty = body.getConsumedQty() != null
                ? body.getConsumedQty() : reservation.getReservedQty();
        AppUser user = body.getUserId() != null
                ? appUserRepository.findById(body.getUserId()).orElse(null) : null;

        reservation.setStatus("CONVERTED");
        reservation.setConvertedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        ChemicalContainer container = reservation.getContainer();
        container.setQuantity(container.getQuantity().subtract(consumedQty));
        container.setStatus(container.getQuantity().compareTo(java.math.BigDecimal.ZERO) <= 0
                ? "CONSUMED" : "AVAILABLE");
        containerRepository.save(container);

        DocumentChemicalConsumption consumption = DocumentChemicalConsumption.builder()
                .tenantId(u.getTenantId())
                .branchId(branchId)
                .worksheetExecution(reservation.getWorksheetExecution())
                .container(container)
                .reservation(reservation)
                .consumedQty(consumedQty)
                .consumedBy(user)
                .consumedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(consumptionRepository.save(consumption));
    }
}
