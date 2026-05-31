package com.sivayahealth.lims.dto.order;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReceiveOrderRequest {
    private BigDecimal deliveredQuantity;
    private String deliveryNotes;
}
