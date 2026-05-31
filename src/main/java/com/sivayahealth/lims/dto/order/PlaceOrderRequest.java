package com.sivayahealth.lims.dto.order;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PlaceOrderRequest {
    private String poNumber;
    private Long supplierId;
    private LocalDate expectedDeliveryDate;
    private String notes;
}
