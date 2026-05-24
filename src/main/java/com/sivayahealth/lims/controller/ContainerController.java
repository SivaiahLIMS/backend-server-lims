package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/containers")
@RequiredArgsConstructor
@Tag(name = "Containers", description = "Chemical container lifecycle with FEFO support")
public class ContainerController {

    private final ChemicalContainerRepository containerRepository;
    private final ChemicalContainerReservationRepository reservationRepository;
    private final DocumentChemicalConsumptionRepository consumptionRepository;
    private final AppUserRepository appUserRepository;

    @GetMapping
    @Operation(summary = "List all chemical containers")
    public List<ChemicalContainer> getContainers(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) String status) {
        return status != null
                ? containerRepository.findByTenantIdAndBranchIdAndStatus(tenantId, branchId, status)
                : containerRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get container by ID")
    public ChemicalContainer getContainer(@PathVariable Long id) {
        return containerRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Container not found: " + id));
    }

    @PostMapping
    @Operation(summary = "Create a new chemical container")
    public ResponseEntity<ChemicalContainer> createContainer(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody ChemicalContainer container) {
        container.setTenantId(tenantId);
        container.setBranchId(branchId);
        return ResponseEntity.status(201).body(containerRepository.save(container));
    }

    @GetMapping("/reservations/fefo-select")
    @Operation(summary = "FEFO-based container selection for a chemical")
    public List<ChemicalContainer> fefoSelect(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam Long chemicalId) {
        return containerRepository.findAvailableByChemicalIdOrderByFEFO(tenantId, branchId, chemicalId);
    }

    @PostMapping("/reservations")
    @Operation(summary = "Reserve a container (FEFO-based)")
    public ResponseEntity<ChemicalContainerReservation> reserveContainer(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, Object> body) {
        Long containerId = Long.valueOf(body.get("containerId").toString());
        BigDecimal qty = new BigDecimal(body.get("reservedQty").toString());
        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : null;

        ChemicalContainer container = containerRepository.findById(containerId)
                .orElseThrow(() -> LimsException.notFound("Container not found: " + containerId));
        AppUser user = userId != null ? appUserRepository.findById(userId).orElse(null) : null;

        ChemicalContainerReservation reservation = ChemicalContainerReservation.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .container(container)
                .reservedQty(qty)
                .status("ACTIVE")
                .reservedBy(user)
                .reservedAt(LocalDateTime.now())
                .build();

        container.setStatus("RESERVED");
        containerRepository.save(container);

        return ResponseEntity.status(201).body(reservationRepository.save(reservation));
    }

    @PostMapping("/reservations/{id}/convert")
    @Operation(summary = "Convert reservation to consumption")
    public ResponseEntity<DocumentChemicalConsumption> convertReservation(
            @PathVariable Long id,
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody Map<String, Object> body) {
        ChemicalContainerReservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> LimsException.notFound("Reservation not found: " + id));

        Long userId = body.containsKey("userId") ? Long.valueOf(body.get("userId").toString()) : null;
        BigDecimal consumedQty = body.containsKey("consumedQty")
                ? new BigDecimal(body.get("consumedQty").toString())
                : reservation.getReservedQty();

        AppUser user = userId != null ? appUserRepository.findById(userId).orElse(null) : null;

        reservation.setStatus("CONVERTED");
        reservation.setConvertedAt(LocalDateTime.now());
        reservationRepository.save(reservation);

        ChemicalContainer container = reservation.getContainer();
        container.setQuantity(container.getQuantity().subtract(consumedQty));
        if (container.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            container.setStatus("CONSUMED");
        } else {
            container.setStatus("AVAILABLE");
        }
        containerRepository.save(container);

        DocumentChemicalConsumption consumption = DocumentChemicalConsumption.builder()
                .tenantId(tenantId)
                .branchId(branchId)
                .worksheetExecution(reservation.getWorksheetExecution())
                .container(container)
                .reservation(reservation)
                .consumedQty(consumedQty)
                .consumedBy(user)
                .consumedAt(LocalDateTime.now())
                .build();

        return ResponseEntity.status(201).body(consumptionRepository.save(consumption));
    }
}
