package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ProductAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttachmentRepository extends JpaRepository<ProductAttachment, Long> {
    List<ProductAttachment> findByProductProductIdAndTenantIdAndBranchId(
            Long productId, Long tenantId, Long branchId);
    Optional<ProductAttachment> findByAttachmentIdAndTenantIdAndBranchId(
            Long attachmentId, Long tenantId, Long branchId);
}
