package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.SupplierRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplierRatingRepository extends JpaRepository<SupplierRating, Long> {
    List<SupplierRating> findBySupplierId(Long supplierId);
}
