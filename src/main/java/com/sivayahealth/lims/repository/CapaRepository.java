package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.Capa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CapaRepository extends JpaRepository<Capa, Long> {
    List<Capa> findByTenantId(Long tenantId);
    List<Capa> findByTenantIdAndStatus(Long tenantId, String status);
    List<Capa> findByDeviationId(Long deviationId);
}
