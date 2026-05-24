package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "category_details")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CategoryDetails {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;
}
