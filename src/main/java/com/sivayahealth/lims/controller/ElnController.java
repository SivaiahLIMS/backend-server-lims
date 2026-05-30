package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.ElnEntry;
import com.sivayahealth.lims.security.LimsUserDetails;
import com.sivayahealth.lims.service.ElnService;
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
@RequestMapping("/eln")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "ELN", description = "Electronic Lab Notebook entries")
public class ElnController {

    private final ElnService elnService;

    @GetMapping
    @PreAuthorize("hasAuthority('ELN_VIEW')")
    @Operation(summary = "List ELN entries for branch",
               description = "Requires: ELN_VIEW. Scoped by X-Branch-Id header. Filter by worksheetId param.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<ElnEntry>> getEntries(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestParam(required = false) Long worksheetId,
            @AuthenticationPrincipal LimsUserDetails u) {
        List<ElnEntry> entries = worksheetId != null
                ? elnService.getEntriesByWorksheet(worksheetId)
                : elnService.getEntries(u.getTenantId(), branchId);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ELN_VIEW')")
    @Operation(summary = "Get ELN entry by ID",
               description = "Requires: ELN_VIEW")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ElnEntry> getEntry(@PathVariable Long id) {
        return ResponseEntity.ok(elnService.getEntry(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ELN_CREATE')")
    @Operation(summary = "Create ELN entry",
               description = "Requires: ELN_CREATE. tenantId and branchId set from JWT and X-Branch-Id header.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created"),
        @ApiResponse(responseCode = "400", description = "Missing required fields"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ElnEntry> createEntry(
            @RequestHeader("X-Branch-Id") Long branchId,
            @RequestBody ElnEntry entry,
            @AuthenticationPrincipal LimsUserDetails u) {
        entry.setTenantId(u.getTenantId());
        entry.setBranchId(branchId);
        return ResponseEntity.status(HttpStatus.CREATED).body(elnService.createEntry(entry));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ELN_CREATE')")
    @Operation(summary = "Update ELN entry",
               description = "Requires: ELN_CREATE")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<ElnEntry> updateEntry(@PathVariable Long id, @RequestBody ElnEntry entry) {
        return ResponseEntity.ok(elnService.updateEntry(id, entry));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ELN_DELETE')")
    @Operation(summary = "Delete ELN entry",
               description = "Requires: ELN_DELETE")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "404", description = "Not found"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<Void> deleteEntry(@PathVariable Long id) {
        elnService.deleteEntry(id);
        return ResponseEntity.noContent().build();
    }
}
