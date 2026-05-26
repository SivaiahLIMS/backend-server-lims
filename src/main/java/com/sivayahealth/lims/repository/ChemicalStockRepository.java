package com.sivayahealth.lims.repository;

import com.sivayahealth.lims.entity.ChemicalStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChemicalStockRepository extends JpaRepository<ChemicalStock, Long> {
    Optional<ChemicalStock> findByRegistrationId(Long registrationId);
    List<ChemicalStock> findByTenantIdAndBranchId(Long tenantId, Long branchId);
    List<ChemicalStock> findByTenantIdAndBranchIdAndStatus(Long tenantId, Long branchId, String status);

    /**
     * Load individual available stock lines for a set of chemical IDs (used for detail enrichment).
     */
    @Query("""
        SELECT s FROM ChemicalStock s
        WHERE s.tenant.id = :tenantId
          AND s.status = 'AVAILABLE'
          AND s.registration.chemical.id IN :chemicalIds
        ORDER BY s.registration.chemical.name ASC, s.registration.expiryDate ASC
        """)
    List<ChemicalStock> findDetailLinesByChemicalIds(
            @Param("tenantId") Long tenantId,
            @Param("chemicalIds") List<Long> chemicalIds);

    /**
     * Aggregated stock per chemical name (case-insensitive LIKE) across all branches of a tenant.
     * Returns rows where the total quantity in stock >= minVolume.
     * Each row: [chemicalId, chemicalName, uomName, totalQuantity, totalContainers]
     */
    @Query("""
        SELECT s.registration.chemical.id,
               s.registration.chemical.name,
               s.registration.uom.name,
               SUM(s.quantityInStock),
               SUM(s.containersInStock)
        FROM ChemicalStock s
        WHERE s.tenant.id = :tenantId
          AND s.status = 'AVAILABLE'
          AND LOWER(s.registration.chemical.name) LIKE LOWER(CONCAT('%', :nameQuery, '%'))
        GROUP BY s.registration.chemical.id, s.registration.chemical.name, s.registration.uom.name
        HAVING SUM(s.quantityInStock) >= :minVolume
        ORDER BY s.registration.chemical.name
        """)
    List<Object[]> findChemicalsByNameAndMinVolume(
            @Param("tenantId") Long tenantId,
            @Param("nameQuery") String nameQuery,
            @Param("minVolume") BigDecimal minVolume);

    /**
     * All available stock lines in a branch where quantity >= minVolume and expiry >= today (or within range).
     * Returns individual stock records so callers see per-registration detail.
     */
    @Query("""
        SELECT s FROM ChemicalStock s
        WHERE s.tenant.id = :tenantId
          AND s.branch.id = :branchId
          AND s.status = 'AVAILABLE'
          AND s.quantityInStock >= :minVolume
          AND s.registration.expiryDate >= :expiryFrom
          AND s.registration.expiryDate <= :expiryTo
        ORDER BY s.registration.expiryDate ASC, s.registration.chemical.name ASC
        """)
    List<ChemicalStock> findAvailableInBranchByExpiryAndVolume(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("minVolume") BigDecimal minVolume,
            @Param("expiryFrom") LocalDate expiryFrom,
            @Param("expiryTo") LocalDate expiryTo);

    /**
     * Aggregated available stock per chemical in a branch with expiry filtering.
     * Groups all registrations of the same chemical; total >= minVolume.
     * Each row: [chemicalId, chemicalName, uomName, totalQuantity, totalContainers, earliestExpiry, latestExpiry]
     */
    @Query("""
        SELECT s.registration.chemical.id,
               s.registration.chemical.name,
               s.registration.uom.name,
               SUM(s.quantityInStock),
               SUM(s.containersInStock),
               MIN(s.registration.expiryDate),
               MAX(s.registration.expiryDate)
        FROM ChemicalStock s
        WHERE s.tenant.id = :tenantId
          AND s.branch.id = :branchId
          AND s.status = 'AVAILABLE'
          AND s.registration.expiryDate >= :expiryFrom
          AND s.registration.expiryDate <= :expiryTo
        GROUP BY s.registration.chemical.id, s.registration.chemical.name, s.registration.uom.name
        HAVING SUM(s.quantityInStock) >= :minVolume
        ORDER BY MIN(s.registration.expiryDate) ASC
        """)
    List<Object[]> findAggregatedAvailableInBranchByExpiryAndVolume(
            @Param("tenantId") Long tenantId,
            @Param("branchId") Long branchId,
            @Param("minVolume") BigDecimal minVolume,
            @Param("expiryFrom") LocalDate expiryFrom,
            @Param("expiryTo") LocalDate expiryTo);
}
