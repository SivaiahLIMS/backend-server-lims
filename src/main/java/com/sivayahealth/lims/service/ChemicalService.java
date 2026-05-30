package com.sivayahealth.lims.service;

import com.sivayahealth.lims.dto.chemical.BranchChemicalAvailability;
import com.sivayahealth.lims.dto.chemical.ChemicalLabelDto;
import com.sivayahealth.lims.dto.chemical.ChemicalSearchResult;
import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChemicalService {

    private final ChemicalMasterRepository chemicalMasterRepository;
    private final ChemicalRegistrationRepository registrationRepository;
    private final ChemicalStockRepository stockRepository;
    private final ChemicalIssuanceRepository issuanceRepository;
    private final ChemicalDestructionRepository destructionRepository;
    private final TenantRepository tenantRepository;
    private final BranchRepository branchRepository;
    private final AppUserRepository userRepository;
    private final UomDetailsRepository uomRepository;
    private final AuditService auditService;
    private final QrCodeService qrCodeService;

    @Transactional(readOnly = true)
    public List<ChemicalMaster> getChemicalMasters(Long tenantId) {
        return chemicalMasterRepository.findByTenantIdAndActiveTrue(tenantId);
    }

    @Transactional
    public ChemicalMaster createChemicalMaster(Long tenantId, ChemicalMaster master) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        master.setTenant(tenant);
        master.setActive(true);
        ChemicalMaster saved = chemicalMasterRepository.save(master);
        auditService.log(tenantId, null, "ChemicalMaster", saved.getId(), "CREATE", null, saved.getName());
        return saved;
    }

    @Transactional
    public ChemicalRegistration registerChemical(Long tenantId, Long branchId,
                                                  ChemicalRegistration registration, Long userId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> LimsException.notFound("User not found"));

        String regNo = generateRegNo(tenantId);
        registration.setTenant(tenant);
        registration.setBranch(branch);
        registration.setReceivedBy(user);
        registration.setRegNo(regNo);
        registration.setStatus("ACTIVE");

        ChemicalRegistration saved = registrationRepository.save(registration);

        ChemicalStock stock = ChemicalStock.builder()
                .tenant(tenant)
                .branch(branch)
                .registration(saved)
                .containersInStock(saved.getNoOfContainers())
                .quantityInStock(saved.getQuantityReceived())
                .status("AVAILABLE")
                .build();
        stockRepository.save(stock);

        auditService.log(tenantId, userId, "ChemicalRegistration", saved.getId(), "REGISTER", null, regNo);
        return saved;
    }

    @Transactional
    public ChemicalIssuance issueChemical(Long tenantId, Long branchId, Long registrationId,
                                           BigDecimal quantity, int containers,
                                           Long issuedToId, Long issuedById, String purpose) {
        ChemicalStock stock = stockRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> LimsException.notFound("Stock not found"));

        if (stock.getQuantityInStock().compareTo(quantity) < 0) {
            throw LimsException.badRequest("Insufficient stock");
        }

        ChemicalRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> LimsException.notFound("Registration not found"));

        AppUser issuedTo = userRepository.findById(issuedToId).orElse(null);
        AppUser issuedBy = userRepository.findById(issuedById).orElse(null);

        stock.setQuantityInStock(stock.getQuantityInStock().subtract(quantity));
        stock.setContainersInStock(stock.getContainersInStock() - containers);
        stock.setLastUpdatedAt(LocalDateTime.now());
        stockRepository.save(stock);

        ChemicalIssuance issuance = ChemicalIssuance.builder()
                .tenant(reg.getTenant())
                .branch(reg.getBranch())
                .registration(reg)
                .containersIssued(containers)
                .issuedQuantity(quantity)
                .uom(reg.getUom())
                .issuedTo(issuedTo)
                .issuedBy(issuedBy)
                .purpose(purpose)
                .build();

        ChemicalIssuance saved = issuanceRepository.save(issuance);
        auditService.log(tenantId, issuedById, "ChemicalIssuance", saved.getId(), "ISSUE", null, reg.getRegNo());
        return saved;
    }

    @Transactional
    public ChemicalDestruction destroyChemical(Long tenantId, Long registrationId,
                                                BigDecimal quantity, int containers,
                                                Long destroyedById, Long witnessedById,
                                                String method, String remarks) {
        ChemicalStock stock = stockRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> LimsException.notFound("Stock not found"));

        if (stock.getQuantityInStock().compareTo(quantity) < 0) {
            throw LimsException.badRequest("Insufficient stock for destruction");
        }

        ChemicalRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> LimsException.notFound("Registration not found"));

        stock.setQuantityInStock(stock.getQuantityInStock().subtract(quantity));
        stock.setContainersInStock(stock.getContainersInStock() - containers);
        stock.setLastUpdatedAt(LocalDateTime.now());
        if (stock.getQuantityInStock().compareTo(BigDecimal.ZERO) == 0) {
            stock.setStatus("DESTROYED");
        }
        stockRepository.save(stock);

        ChemicalDestruction destruction = ChemicalDestruction.builder()
                .tenant(reg.getTenant())
                .branch(reg.getBranch())
                .registration(reg)
                .containersDestroyed(containers)
                .quantityDestroyed(quantity)
                .uom(reg.getUom())
                .destroyedBy(destroyedById != null ? userRepository.findById(destroyedById).orElse(null) : null)
                .witnessedBy(witnessedById != null ? userRepository.findById(witnessedById).orElse(null) : null)
                .method(method)
                .remarks(remarks)
                .build();

        ChemicalDestruction saved = destructionRepository.save(destruction);
        auditService.log(tenantId, destroyedById, "ChemicalDestruction", saved.getId(), "DESTROY", null, reg.getRegNo());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<ChemicalRegistration> getExpiringChemicals(Long tenantId, int daysAhead) {
        return registrationRepository.findExpiringChemicals(tenantId, LocalDate.now().plusDays(daysAhead));
    }

    @Transactional(readOnly = true)
    public List<ChemicalStock> getStockByBranch(Long tenantId, Long branchId) {
        return stockRepository.findByTenantIdAndBranchId(tenantId, branchId);
    }

    // ── Search queries ──────────────────────────────────────────────────────

    /**
     * Search chemicals by name (partial, case-insensitive) across all branches of a tenant.
     * Only includes chemicals where the total available stock >= minVolume.
     * Returns aggregated totals plus per-registration detail lines.
     */
    @Transactional(readOnly = true)
    public List<ChemicalSearchResult> searchByNameAndVolume(Long tenantId, String nameQuery, BigDecimal minVolume) {
        List<Object[]> aggregates = stockRepository.findChemicalsByNameAndMinVolume(tenantId, nameQuery, minVolume);

        // Collect chemical IDs from aggregate result, then load detail lines in one query
        List<Long> chemicalIds = aggregates.stream()
                .map(row -> ((Number) row[0]).longValue())
                .toList();

        if (chemicalIds.isEmpty()) return List.of();

        List<ChemicalStock> detailLines = stockRepository.findDetailLinesByChemicalIds(tenantId, chemicalIds);

        Map<Long, List<ChemicalStock>> byChemical = detailLines.stream()
                .collect(Collectors.groupingBy(s -> s.getRegistration().getChemical().getId()));

        return aggregates.stream().map(row -> {
            Long chemId   = ((Number) row[0]).longValue();
            String name   = (String) row[1];
            String uom    = (String) row[2];
            BigDecimal total    = (BigDecimal) row[3];
            int containers      = ((Number) row[4]).intValue();

            List<ChemicalSearchResult.RegistrationLine> lines = byChemical.getOrDefault(chemId, List.of())
                    .stream()
                    .map(s -> new ChemicalSearchResult.RegistrationLine(
                            s.getRegistration().getId(),
                            s.getRegistration().getRegNo(),
                            s.getQuantityInStock(),
                            s.getContainersInStock(),
                            s.getRegistration().getExpiryDate() != null
                                    ? s.getRegistration().getExpiryDate().toString() : null,
                            s.getRegistration().getLotNo(),
                            s.getRegistration().getGrade() != null
                                    ? s.getRegistration().getGrade().getName() : null,
                            s.getBranch().getName()
                    ))
                    .toList();

            return new ChemicalSearchResult(chemId, name, uom, total, containers, lines);
        }).toList();
    }

    /**
     * Return available chemicals in a specific branch filtered by expiry window and minimum volume.
     * Groups by chemical master; shows summary totals and per-registration detail.
     */
    @Transactional(readOnly = true)
    public BranchChemicalAvailability getAvailableInBranch(Long tenantId, Long branchId,
                                                            BigDecimal minVolume,
                                                            LocalDate expiryFrom,
                                                            LocalDate expiryTo) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> LimsException.notFound("Branch not found"));

        List<Object[]> aggregates = stockRepository.findAggregatedAvailableInBranchByExpiryAndVolume(
                tenantId, branchId, minVolume, expiryFrom, expiryTo);

        // detail lines for the same filter
        List<ChemicalStock> detailLines = stockRepository.findAvailableInBranchByExpiryAndVolume(
                tenantId, branchId, minVolume, expiryFrom, expiryTo);

        Map<Long, List<ChemicalStock>> byChemical = detailLines.stream()
                .collect(Collectors.groupingBy(s -> s.getRegistration().getChemical().getId()));

        List<BranchChemicalAvailability.ChemicalSummary> chemicals = aggregates.stream().map(row -> {
            Long chemId          = ((Number) row[0]).longValue();
            String name          = (String) row[1];
            String uom           = (String) row[2];
            BigDecimal total     = (BigDecimal) row[3];
            int containers       = ((Number) row[4]).intValue();
            LocalDate earliest   = (LocalDate) row[5];
            LocalDate latest     = (LocalDate) row[6];

            List<BranchChemicalAvailability.RegistrationLine> lines =
                    byChemical.getOrDefault(chemId, List.of()).stream()
                            .map(s -> new BranchChemicalAvailability.RegistrationLine(
                                    s.getRegistration().getId(),
                                    s.getRegistration().getRegNo(),
                                    s.getQuantityInStock(),
                                    s.getContainersInStock(),
                                    s.getRegistration().getExpiryDate() != null
                                            ? s.getRegistration().getExpiryDate().toString() : null,
                                    s.getRegistration().getLotNo(),
                                    s.getRegistration().getGrade() != null
                                            ? s.getRegistration().getGrade().getName() : null,
                                    s.getRegistration().getStorageCondition() != null
                                            ? s.getRegistration().getStorageCondition().getValue() : null
                            ))
                            .toList();

            return new BranchChemicalAvailability.ChemicalSummary(
                    chemId, name, uom, total, containers,
                    earliest != null ? earliest.toString() : null,
                    latest != null ? latest.toString() : null,
                    lines
            );
        }).toList();

        return new BranchChemicalAvailability(branchId, branch.getName(), chemicals);
    }

    /**
     * Return available chemicals in a branch expiring within the next {@code daysAhead} days
     * with stock >= minVolume. Convenience wrapper over getAvailableInBranch.
     */
    @Transactional(readOnly = true)
    public BranchChemicalAvailability getAvailableInBranchExpiringSoon(Long tenantId, Long branchId,
                                                                        BigDecimal minVolume,
                                                                        int daysAhead) {
        LocalDate today = LocalDate.now();
        return getAvailableInBranch(tenantId, branchId, minVolume, today, today.plusDays(daysAhead));
    }

    private String generateRegNo(Long tenantId) {
        Long seq = registrationRepository.findMaxRegNoSeq(tenantId);
        return String.format("CA%08d", seq + 1);
    }

    @Transactional(readOnly = true)
    public byte[] getRegistrationQrPng(Long tenantId, Long registrationId) {
        ChemicalRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> LimsException.notFound("Registration not found"));
        if (!reg.getTenant().getId().equals(tenantId)) {
            throw LimsException.notFound("Registration not found");
        }
        String payload = buildQrPayload(reg);
        return qrCodeService.generateQrPng(payload);
    }

    @Transactional(readOnly = true)
    public ChemicalLabelDto getRegistrationLabel(Long tenantId, Long registrationId) {
        ChemicalRegistration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> LimsException.notFound("Registration not found"));
        if (!reg.getTenant().getId().equals(tenantId)) {
            throw LimsException.notFound("Registration not found");
        }
        return buildLabel(reg);
    }

    @Transactional(readOnly = true)
    public List<ChemicalLabelDto> getRegistrationLabels(Long tenantId, List<Long> registrationIds) {
        return registrationIds.stream()
                .map(id -> registrationRepository.findById(id)
                        .filter(r -> r.getTenant().getId().equals(tenantId))
                        .orElseThrow(() -> LimsException.notFound("Registration not found: " + id)))
                .map(this::buildLabel)
                .toList();
    }

    private ChemicalLabelDto buildLabel(ChemicalRegistration reg) {
        String chemName = reg.getChemical() != null ? reg.getChemical().getName() : "";
        String prefix = chemName.length() >= 4 ? chemName.substring(0, 4).toUpperCase()
                : chemName.toUpperCase();

        String payload = buildQrPayload(reg);
        String qrBase64 = qrCodeService.generateQrBase64(payload);

        return ChemicalLabelDto.builder()
                .registrationId(reg.getId())
                .regNo(reg.getRegNo())
                .chemicalName(chemName)
                .chemicalPrefix(prefix)
                .casNo(reg.getChemical() != null ? reg.getChemical().getCasNo() : null)
                .lotNo(reg.getLotNo())
                .grade(reg.getGrade() != null ? reg.getGrade().getName() : null)
                .category(reg.getCategory() != null ? reg.getCategory().getName() : null)
                .hazardClass(reg.getChemical() != null ? reg.getChemical().getHazardClass() : null)
                .quantity(reg.getQuantityReceived() != null ? reg.getQuantityReceived().toPlainString() : null)
                .uom(reg.getUom() != null ? reg.getUom().getName() : null)
                .mfgDate(reg.getMfgDate() != null ? reg.getMfgDate().toString() : null)
                .expiryDate(reg.getExpiryDate() != null ? reg.getExpiryDate().toString() : null)
                .receivedDate(reg.getReceivedDate() != null ? reg.getReceivedDate().toString() : null)
                .storageCondition(reg.getStorageCondition() != null ? reg.getStorageCondition().getValue() : null)
                .tenantId(reg.getTenant().getId())
                .tenantName(reg.getTenant().getName())
                .branchId(reg.getBranch().getId())
                .branchName(reg.getBranch().getName())
                .qrPayload(payload)
                .qrBase64(qrBase64)
                .build();
    }

    private String buildQrPayload(ChemicalRegistration reg) {
        String chemName = reg.getChemical() != null ? reg.getChemical().getName() : "CHEM";
        String expiryStr = reg.getExpiryDate() != null ? reg.getExpiryDate().toString() : "N/A";
        return qrCodeService.buildChemicalQrPayload(
                chemName,
                reg.getTenant().getId(),
                reg.getBranch().getId(),
                reg.getRegNo(),
                expiryStr
        );
    }
}
