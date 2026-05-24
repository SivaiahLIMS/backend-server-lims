package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.QaService;
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
@RequestMapping("/qa")
@RequiredArgsConstructor
@Tag(name = "QA/QC Module", description = "Deviations, OOS, CAPA management")
public class QaController {

    private final QaService qaService;

    @GetMapping("/deviations")
    @PreAuthorize("hasAuthority('DEVIATION_VIEW')")
    @Operation(summary = "Get deviations")
    public ResponseEntity<List<Deviation>> getDeviations(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getDeviations(u.getTenantId(), branchId));
    }

    @PostMapping("/deviations")
    @PreAuthorize("hasAuthority('DEVIATION_CREATE')")
    @Operation(summary = "Create a deviation")
    public ResponseEntity<Deviation> createDeviation(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                qaService.createDeviation(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        (String) body.get("refEntity"),
                        body.containsKey("refId") ? ((Number) body.get("refId")).longValue() : null,
                        (String) body.get("description"),
                        (String) body.get("severity"),
                        u.getUser().getId()
                )
        );
    }

    @PostMapping("/deviations/{id}/close")
    @PreAuthorize("hasAuthority('DEVIATION_CLOSE')")
    @Operation(summary = "Close a deviation")
    public ResponseEntity<Deviation> closeDeviation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeDeviation(id, u.getUser().getId(), body.get("remarks")));
    }

    @GetMapping("/oos")
    @PreAuthorize("hasAuthority('OOS_VIEW')")
    @Operation(summary = "Get OOS cases")
    public ResponseEntity<List<OosCase>> getOos(
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getOosCases(u.getTenantId(), branchId));
    }

    @PostMapping("/oos")
    @PreAuthorize("hasAuthority('OOS_CREATE')")
    @Operation(summary = "Create an OOS case")
    public ResponseEntity<OosCase> createOos(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                qaService.createOos(
                        u.getTenantId(),
                        ((Number) body.get("branchId")).longValue(),
                        body.containsKey("sampleId") ? ((Number) body.get("sampleId")).longValue() : null,
                        body.containsKey("testId") ? ((Number) body.get("testId")).longValue() : null,
                        (String) body.get("description"),
                        u.getUser().getId()
                )
        );
    }

    @GetMapping("/capa")
    @PreAuthorize("hasAuthority('CAPA_VIEW')")
    @Operation(summary = "Get CAPA list")
    public ResponseEntity<List<Capa>> getCapa(@AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.getCapas(u.getTenantId()));
    }

    @PostMapping("/capa")
    @PreAuthorize("hasAuthority('CAPA_CREATE')")
    @Operation(summary = "Create a CAPA")
    public ResponseEntity<Capa> createCapa(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                qaService.createCapa(
                        u.getTenantId(),
                        body.containsKey("deviationId") ? ((Number) body.get("deviationId")).longValue() : null,
                        (String) body.get("actionDesc"),
                        body.containsKey("ownerId") ? ((Number) body.get("ownerId")).longValue() : null,
                        body.containsKey("dueDate") ? LocalDate.parse((String) body.get("dueDate")) : null
                )
        );
    }

    @PostMapping("/capa/{id}/close")
    @PreAuthorize("hasAuthority('CAPA_CLOSE')")
    @Operation(summary = "Close a CAPA")
    public ResponseEntity<Capa> closeCapa(@PathVariable Long id,
                                           @RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal LimsUserDetails u) {
        return ResponseEntity.ok(qaService.closeCapa(id, body.get("remarks"), u.getUser().getId()));
    }
}
