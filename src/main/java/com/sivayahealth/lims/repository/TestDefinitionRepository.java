package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.TestDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TestDefinitionRepository extends JpaRepository<TestDefinition, Long> {
    List<TestDefinition> findByTenantIdAndStatus(Long tenantId, String status);
}
