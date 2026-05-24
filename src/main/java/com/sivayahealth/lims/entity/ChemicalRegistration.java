package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "chemical_registration")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chemical_id", nullable = false)
    private ChemicalMaster chemical;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryDetails category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_id")
    private GradeDetails grade;

    @Column(name = "cas_cat_no", length = 100)
    private String casCatNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manufacturer_id")
    private Supplier manufacturer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(name = "lot_no", length = 100)
    private String lotNo;

    @Column(name = "delivery_receipt_no", length = 100)
    private String deliveryReceiptNo;

    @Column(name = "no_of_containers", nullable = false)
    private Integer noOfContainers;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uom_id", nullable = false)
    private UomDetails uom;

    @Column(name = "quantity_received", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantityReceived;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_condition_id")
    private StorageConditionDetails storageCondition;

    @Column(name = "mfg_date")
    private LocalDate mfgDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by")
    private AppUser receivedBy;

    @Column(name = "reg_no", unique = true, nullable = false, length = 50)
    private String regNo;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";
}
