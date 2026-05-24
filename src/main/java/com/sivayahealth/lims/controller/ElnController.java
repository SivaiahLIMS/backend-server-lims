package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.ElnEntry;
import com.sivayahealth.lims.service.ElnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/eln")
@RequiredArgsConstructor
@Tag(name = "ELN", description = "Electronic Lab Notebook entries")
public class ElnController {

    private final ElnService elnService;

    @GetMapping
    @Operation(summary = "List ELN entries")
    public List<ElnEntry> getEntries(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestParam(required = false) Long worksheetId) {
        return worksheetId != null
                ? elnService.getEntriesByWorksheet(worksheetId)
                : elnService.getEntries(tenantId, branchId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ELN entry by ID")
    public ElnEntry getEntry(@PathVariable Long id) {
        return elnService.getEntry(id);
    }

    @PostMapping
    @Operation(summary = "Create ELN entry")
    public ResponseEntity<ElnEntry> createEntry(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @RequestHeader(value = "X-Branch-Id", required = false) Long branchId,
            @RequestBody ElnEntry entry) {
        entry.setTenantId(tenantId);
        entry.setBranchId(branchId);
        return ResponseEntity.status(201).body(elnService.createEntry(entry));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update ELN entry")
    public ElnEntry updateEntry(@PathVariable Long id, @RequestBody ElnEntry entry) {
        return elnService.updateEntry(id, entry);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete ELN entry")
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        elnService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
}
