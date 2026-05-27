package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.OrderRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRequestRepository extends JpaRepository<OrderRequest, Long> {

    List<OrderRequest> findByTenantIdAndBranchId(Long tenantId, Long branchId);

    List<OrderRequest> findByTenantIdAndStatus(Long tenantId, String status);

    List<OrderRequest> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);

    List<OrderRequest> findByTenantIdAndRequestType(Long tenantId, String requestType);

    /** Items expected (ORDER_PLACED) with expected delivery date within range — "due for delivery" list. */
    @Query("""
        SELECT o FROM OrderRequest o
        WHERE o.tenant.id = :tenantId
          AND o.status = 'ORDER_PLACED'
          AND o.expectedDeliveryDate IS NOT NULL
          AND o.expectedDeliveryDate <= :upTo
        ORDER BY o.expectedDeliveryDate ASC
        """)
    List<OrderRequest> findDueForDelivery(@Param("tenantId") Long tenantId, @Param("upTo") LocalDate upTo);

    /** All requests raised by a specific user. */
    List<OrderRequest> findByRequestedByIdAndTenantId(Long userId, Long tenantId);

    List<OrderRequest> findByTenantId(Long tenantId);
}
