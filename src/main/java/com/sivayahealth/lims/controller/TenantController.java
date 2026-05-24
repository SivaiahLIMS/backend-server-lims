package com.sivayahealth.lims.controller;

import com.sivayahealth.lims.entity.Branch;
import com.sivayahealth.lims.entity.Tenant;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.BranchRepository;
import com.sivayahealth.lims.repository.TenantRepository;
import com.sivayahealth.lims.security.LimsUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "Tenant & Branch", description = "Tenant and branch management")
public class TenantController {

    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_VIEW')")
    @Operation(summary = "Get all tenants")
    public ResponseEntity<List<Tenant>> getTenants() {
        return ResponseEntity.ok(tenantRepository.findAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TENANT_CREATE')")
    @Operation(summary = "Create a tenant")
    public ResponseEntity<Tenant> createTenant(@RequestBody Tenant tenant) {
        if (tenantRepository.existsByCode(tenant.getCode())) {
            throw LimsException.conflict("Tenant code already exists");
        }
        tenant.setStatus("ACTIVE");
        return ResponseEntity.status(HttpStatus.CREATED).body(tenantRepository.save(tenant));
    }

    @GetMapping("/{tenantId}/branches")
    @PreAuthorize("hasAuthority('BRANCH_VIEW')")
    @Operation(summary = "Get branches for a tenant")
    public ResponseEntity<List<Branch>> getBranches(@PathVariable Long tenantId) {
        return ResponseEntity.ok(branchRepository.findByTenantId(tenantId));
    }

    @PostMapping("/{tenantId}/branches")
    @PreAuthorize("hasAuthority('BRANCH_CREATE')")
    @Operation(summary = "Create a branch")
    public ResponseEntity<Branch> createBranch(@PathVariable Long tenantId, @RequestBody Branch branch) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        branch.setTenant(tenant);
        branch.setStatus("ACTIVE");
        return ResponseEntity.status(HttpStatus.CREATED).body(branchRepository.save(branch));
    }
}
