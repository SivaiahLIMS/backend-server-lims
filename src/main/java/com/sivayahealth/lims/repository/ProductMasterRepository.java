package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMasterRepository extends JpaRepository<ProductMaster, Long> {

    List<ProductMaster> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    List<ProductMaster> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);

    Optional<ProductMaster> findByTenant_IdAndProductCode(Long tenantId, String productCode);

    boolean existsByTenant_IdAndProductCode(Long tenantId, String productCode);

    @Query("""
        SELECT p FROM ProductMaster p
        WHERE p.tenant.id = :tenantId
          AND p.branch.id = :branchId
          AND (:status IS NULL OR p.status = :status)
          AND (:productName IS NULL OR LOWER(p.productName) LIKE LOWER(CONCAT('%',:productName,'%')))
          AND (:productType IS NULL OR p.productType = :productType)
        ORDER BY p.createdAt DESC
        """)
    List<ProductMaster> search(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("status") String status,
            @Param("productName") String productName,
            @Param("productType") String productType);
}
