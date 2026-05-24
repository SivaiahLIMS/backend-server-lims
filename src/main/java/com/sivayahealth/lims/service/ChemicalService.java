package com.sivayahealth.lims.service;

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

    private String generateRegNo(Long tenantId) {
        Long seq = registrationRepository.findMaxRegNoSeq(tenantId);
        return String.format("CA%08d", seq + 1);
    }
}
