package com.sivayahealth.lims.dto.oms;

import lombok.Data;

@Data
public class CreatePurchaseOrderRequest {
    private Long branchId;
    private Long supplierId;
    private String poNo;
}
