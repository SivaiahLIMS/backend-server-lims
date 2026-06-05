package com.sivayahealth.lims.dto.inventory;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateReagentLotRequest {
    private Long reagentId;
    private String lotNumber;
    private String supplierLot;
    private BigDecimal receivedQty;
    private String uom;
    private LocalDate receivedDate;
    private LocalDate expiryDate;
    private LocalDate manufactureDate;
    private Long supplierId;
    private String storageLocation;
    private String certificateNo;
}
