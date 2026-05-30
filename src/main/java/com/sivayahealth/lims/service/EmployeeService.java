package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeMasterRepository employeeRepo;
    private final EmployeeHierarchyRepository hierarchyRepo;
    private final EmployeeAuditRepository auditRepo;
    private final TenantRepository tenantRepo;
    private final BranchRepository branchRepo;
    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;

    // ── List / Lookup ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeMaster> listAll(Long tenantId, Long branchId) {
        return employeeRepo.findByTenantIdAndBranchId(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeMaster> listActive(Long tenantId, Long branchId) {
        return employeeRepo.findByTenantIdAndBranchIdAndIsActive(tenantId, branchId, true);
    }

    @Transactional(readOnly = true)
    public EmployeeMaster getById(Long tenantId, Long branchId, Long employeeId) {
        EmployeeMaster e = employeeRepo.findById(employeeId)
                .orElseThrow(() -> LimsException.notFound("Employee not found"));
        assertScope(e, tenantId, branchId);
        return e;
    }

    @Transactional(readOnly = true)
    public List<EmployeeMaster> getEligibleForAssignment(Long tenantId, Long branchId) {
        return employeeRepo.findEligibleForAssignment(tenantId, branchId);
    }

    @Transactional(readOnly = true)
    public List<EmployeeMaster> getDirectReports(Long tenantId, Long branchId, Long managerId) {
        return employeeRepo.findDirectReports(tenantId, branchId, managerId);
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Transactional
    public EmployeeMaster create(Long tenantId, Long branchId, Long createdByUserId,
                                 EmployeeMaster data) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepo.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser createdBy = userRepo.findById(createdByUserId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (employeeRepo.existsByTenant_IdAndEmployeeCode(tenantId, data.getEmployeeCode())) {
            throw LimsException.conflict("Employee code already exists: " + data.getEmployeeCode());
        }
        if (employeeRepo.existsByTenant_IdAndEmail(tenantId, data.getEmail())) {
            throw LimsException.conflict("Email already registered: " + data.getEmail());
        }

        data.setTenant(tenant);
        data.setBranch(branch);
        data.setCreatedBy(createdBy);
        data.setActive(true);

        return employeeRepo.save(data);
    }

    @Transactional
    public EmployeeMaster update(Long tenantId, Long branchId, Long employeeId,
                                 Long modifierId, Map<String, Object> fields) {
        EmployeeMaster emp = getById(tenantId, branchId, employeeId);
        AppUser modifier = userRepo.findById(modifierId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        if (fields.containsKey("firstName")) {
            logAudit(emp, "firstName", emp.getFirstName(), (String) fields.get("firstName"), modifier);
            emp.setFirstName((String) fields.get("firstName"));
        }
        if (fields.containsKey("lastName")) {
            logAudit(emp, "lastName", emp.getLastName(), (String) fields.get("lastName"), modifier);
            emp.setLastName((String) fields.get("lastName"));
        }
        if (fields.containsKey("phone")) {
            logAudit(emp, "phone", emp.getPhone(), (String) fields.get("phone"), modifier);
            emp.setPhone((String) fields.get("phone"));
        }
        if (fields.containsKey("designation")) {
            logAudit(emp, "designation", emp.getDesignation(), (String) fields.get("designation"), modifier);
            emp.setDesignation((String) fields.get("designation"));
        }
        if (fields.containsKey("roleId")) {
            Long roleId = ((Number) fields.get("roleId")).longValue();
            Role role = roleRepo.findById(roleId)
                    .orElseThrow(() -> LimsException.notFound("Role not found"));
            logAudit(emp, "roleId",
                    emp.getRole() != null ? emp.getRole().getId().toString() : null,
                    roleId.toString(), modifier);
            emp.setRole(role);
        }
        if (fields.containsKey("managerId")) {
            Long mId = fields.get("managerId") != null
                    ? ((Number) fields.get("managerId")).longValue() : null;
            EmployeeMaster mgr = mId != null ? employeeRepo.findById(mId).orElse(null) : null;
            logAudit(emp, "managerId",
                    emp.getManager() != null ? emp.getManager().getEmployeeId().toString() : null,
                    mId != null ? mId.toString() : null, modifier);
            emp.setManager(mgr);
        }
        if (fields.containsKey("reviewerId")) {
            Long rId = fields.get("reviewerId") != null
                    ? ((Number) fields.get("reviewerId")).longValue() : null;
            EmployeeMaster rev = rId != null ? employeeRepo.findById(rId).orElse(null) : null;
            emp.setReviewer(rev);
        }

        emp.setModifiedBy(modifier);
        emp.setModifiedAt(LocalDateTime.now());
        return employeeRepo.save(emp);
    }

    @Transactional
    public EmployeeMaster deactivate(Long tenantId, Long branchId, Long employeeId, Long userId) {
        EmployeeMaster emp = getById(tenantId, branchId, employeeId);
        AppUser actor = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        logAudit(emp, "isActive", "true", "false", actor);
        emp.setActive(false);
        emp.setModifiedBy(actor);
        emp.setModifiedAt(LocalDateTime.now());
        return employeeRepo.save(emp);
    }

    @Transactional
    public EmployeeMaster activate(Long tenantId, Long branchId, Long employeeId, Long userId) {
        EmployeeMaster emp = getById(tenantId, branchId, employeeId);
        AppUser actor = userRepo.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));
        logAudit(emp, "isActive", "false", "true", actor);
        emp.setActive(true);
        emp.setModifiedBy(actor);
        emp.setModifiedAt(LocalDateTime.now());
        return employeeRepo.save(emp);
    }

    // ── Hierarchy ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeHierarchy> getHierarchy(Long tenantId, Long branchId, Long employeeId) {
        return hierarchyRepo.findByEmployee_EmployeeIdAndTenantIdAndBranchId(
                employeeId, tenantId, branchId);
    }

    @Transactional
    public EmployeeHierarchy assignManager(Long tenantId, Long branchId,
                                           Long employeeId, Long managerId, int level) {
        EmployeeMaster employee = getById(tenantId, branchId, employeeId);
        EmployeeMaster manager  = getById(tenantId, branchId, managerId);

        if (employee.getEmployeeId().equals(managerId)) {
            throw LimsException.badRequest("Employee cannot be their own manager");
        }

        // Update the direct manager_id shortcut on the employee record
        employee.setManager(manager);
        employeeRepo.save(employee);

        // Upsert in hierarchy table
        List<EmployeeHierarchy> existing = hierarchyRepo
                .findByEmployee_EmployeeIdAndTenantIdAndBranchId(employeeId, tenantId, branchId);
        existing.stream()
                .filter(h -> h.getManager().getEmployeeId().equals(managerId))
                .findFirst()
                .ifPresent(hierarchyRepo::delete);

        return hierarchyRepo.save(EmployeeHierarchy.builder()
                .tenant(employee.getTenant())
                .branch(employee.getBranch())
                .employee(employee)
                .manager(manager)
                .level(level)
                .build());
    }

    @Transactional
    public void removeManager(Long tenantId, Long branchId, Long employeeId, Long managerId) {
        EmployeeMaster emp = getById(tenantId, branchId, employeeId);
        if (emp.getManager() != null && emp.getManager().getEmployeeId().equals(managerId)) {
            emp.setManager(null);
            employeeRepo.save(emp);
        }
        hierarchyRepo.deleteByEmployee_EmployeeIdAndManager_EmployeeId(employeeId, managerId);
    }

    @Transactional
    public EmployeeMaster assignReviewer(Long tenantId, Long branchId,
                                         Long employeeId, Long reviewerId) {
        EmployeeMaster emp = getById(tenantId, branchId, employeeId);
        EmployeeMaster rev = getById(tenantId, branchId, reviewerId);
        emp.setReviewer(rev);
        return employeeRepo.save(emp);
    }

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EmployeeAudit> getAuditTrail(Long tenantId, Long branchId, Long employeeId) {
        assertExists(tenantId, branchId, employeeId);
        return auditRepo.findByEmployee_EmployeeIdAndTenantIdAndBranchIdOrderByChangedAtDesc(
                employeeId, tenantId, branchId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void assertScope(EmployeeMaster e, Long tenantId, Long branchId) {
        if (!e.getTenant().getId().equals(tenantId) || !e.getBranch().getId().equals(branchId)) {
            throw LimsException.notFound("Employee not found");
        }
    }

    private void assertExists(Long tenantId, Long branchId, Long employeeId) {
        getById(tenantId, branchId, employeeId);
    }

    private void logAudit(EmployeeMaster emp, String field, String oldVal, String newVal, AppUser by) {
        if (oldVal != null && oldVal.equals(newVal)) return;
        auditRepo.save(EmployeeAudit.builder()
                .tenant(emp.getTenant())
                .branch(emp.getBranch())
                .employee(emp)
                .fieldName(field)
                .oldValue(oldVal)
                .newValue(newVal)
                .changedBy(by)
                .build());
    }
}
