package com.sivayahealth.lims.dto.chemical;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result for "search chemicals by name + minimum volume" query.
 * Aggregated across all registrations of the same chemical master.
 */
public record ChemicalSearchResult(
        Long chemicalId,
        String chemicalName,
        String uom,
        BigDecimal totalQuantityInStock,
        int totalContainersInStock,
        List<RegistrationLine> registrations
) {
    public record RegistrationLine(
            Long registrationId,
            String regNo,
            BigDecimal quantityInStock,
            int containersInStock,
            String expiryDate,
            String lotNo,
            String grade,
            String branchName
    ) {}
}
