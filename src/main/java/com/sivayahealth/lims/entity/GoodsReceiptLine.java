package com.sivayahealth.lims.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "goods_receipt_line")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GoodsReceiptLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "po_line_id", nullable = false)
    private PurchaseOrderLine poLine;

    @Column(name = "received_qty", nullable = false, precision = 18, scale = 4)
    private BigDecimal receivedQty;

    @Column(name = "inventory_stock_id")
    private Long inventoryStockId;

    @Column(columnDefinition = "TEXT")
    private String remarks;
}
