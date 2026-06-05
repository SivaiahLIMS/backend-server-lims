package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TestMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestMethodRepository extends JpaRepository<TestMethod, Long> {
    List<TestMethod> findByTenantIdAndActiveTrue(Long tenantId);
}
