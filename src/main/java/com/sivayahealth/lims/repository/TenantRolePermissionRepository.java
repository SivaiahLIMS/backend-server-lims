package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TenantRolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantRolePermissionRepository extends JpaRepository<TenantRolePermission, Long> {

    @Query("""
        SELECT DISTINCT p.code FROM TenantRolePermission trp
        JOIN trp.permission p
        WHERE trp.tenant.id = :tenantId AND trp.role.id IN :roleIds
        """)
    List<String> findPermissionCodesByTenantAndRoles(
            @Param("tenantId") Long tenantId,
            @Param("roleIds") List<Long> roleIds);

    List<TenantRolePermission> findByTenantIdAndRoleId(Long tenantId, Long roleId);
}
