package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_movement")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InventoryMovement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lot_id", nullable = false)
    private InventoryReagentLot lot;

    @Column(name = "movement_type", nullable = false, length = 30)
    private String movementType;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "qty_before", precision = 18, scale = 4)
    private BigDecimal qtyBefore;

    @Column(name = "qty_after", precision = 18, scale = 4)
    private BigDecimal qtyAfter;

    @Column(name = "ref_entity", length = 100)
    private String refEntity;

    @Column(name = "ref_id")
    private Long refId;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", nullable = false)
    private AppUser performedBy;

    @Column(name = "performed_at", nullable = false)
    @Builder.Default
    private LocalDateTime performedAt = LocalDateTime.now();
}
