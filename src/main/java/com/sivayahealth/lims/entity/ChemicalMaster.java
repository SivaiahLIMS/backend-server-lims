package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chemical_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChemicalMaster {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "cas_no", length = 100)
    private String casNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_category_id")
    private CategoryDetails defaultCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_grade_id")
    private GradeDetails defaultGrade;

    @Column(name = "hazard_class", length = 100)
    private String hazardClass;

    @Column(name = "ghs_pictograms", length = 200)
    private String ghsPictograms;

    @Column(name = "nfpa_rating", length = 50)
    private String nfpaRating;

    @Column(name = "sds_file_id")
    private Long sdsFileId;

    @Column(nullable = false)
    private Boolean active = true;
}
