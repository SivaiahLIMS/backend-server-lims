package com.sivayahealth.lims.dto.chemical;

import java.math.BigDecimal;
import java.util.List;

/**
 * Result for "available chemicals in a branch by expiry window and minimum volume".
 * Summary view: one row per chemical master with aggregated stock,
 * plus detailed registration lines.
 */
public record BranchChemicalAvailability(
        Long branchId,
        String branchName,
        List<ChemicalSummary> chemicals
) {
    public record ChemicalSummary(
            Long chemicalId,
            String chemicalName,
            String uom,
            BigDecimal totalQuantityInStock,
            int totalContainersInStock,
            String earliestExpiry,
            String latestExpiry,
            List<RegistrationLine> registrations
    ) {}

    public record RegistrationLine(
            Long registrationId,
            String regNo,
            BigDecimal quantityInStock,
            int containersInStock,
            String expiryDate,
            String lotNo,
            String grade,
            String storageCondition
    ) {}
}
