package com.sivayahealth.lims.service;

import com.sivayahealth.lims.entity.*;
import com.sivayahealth.lims.exception.LimsException;
import com.sivayahealth.lims.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierDocumentRepository documentRepository;
    private final SupplierRatingRepository ratingRepository;
    private final TenantRepository tenantRepository;
    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional
    public Supplier createSupplier(Long tenantId, Supplier supplier, Long createdById) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> LimsException.notFound("Tenant not found"));
        supplier.setTenant(tenant);
        supplier.setStatus("ACTIVE");
        AppUser creator = userRepository.findById(createdById).orElse(null);
        supplier.setCreatedBy(creator);
        Supplier saved = supplierRepository.save(supplier);
        auditService.log(tenantId, createdById, "Supplier", saved.getId(), "CREATE", null, saved.getName());
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Supplier> getSuppliers(Long tenantId) {
        return supplierRepository.findByTenantIdAndStatus(tenantId, "ACTIVE");
    }

    @Transactional
    public SupplierRating rateSupplier(Long supplierId, Integer rating, String remarks, Long ratedById) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> LimsException.notFound("Supplier not found"));
        if (rating < 1 || rating > 5) {
            throw LimsException.badRequest("Rating must be between 1 and 5");
        }
        AppUser ratedBy = ratedById != null ? userRepository.findById(ratedById).orElse(null) : null;

        SupplierRating supplierRating = SupplierRating.builder()
                .supplier(supplier).rating(rating)
                .remarks(remarks).ratedBy(ratedBy)
                .build();
        return ratingRepository.save(supplierRating);
    }

    @Transactional
    public SupplierDocument addDocument(Long supplierId, String docType, Long fileId,
                                         String version, java.time.LocalDate expiryDate, Long uploadedById) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> LimsException.notFound("Supplier not found"));
        AppUser uploader = uploadedById != null ? userRepository.findById(uploadedById).orElse(null) : null;

        SupplierDocument doc = SupplierDocument.builder()
                .supplier(supplier).docType(docType)
                .fileId(fileId).version(version)
                .expiryDate(expiryDate).uploadedBy(uploader)
                .build();
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<SupplierDocument> getDocuments(Long supplierId) {
        return documentRepository.findBySupplierId(supplierId);
    }
}
