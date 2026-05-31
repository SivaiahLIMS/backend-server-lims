package com.sivayahealth.lims.dto.order;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateOrderRequest {
    private String requestType;
    private Long chemicalId;
    private Long instrumentId;
    private BigDecimal quantity;
    private Long uomId;
    private String reason;
    private Long supplierId;
    private LocalDate requiredByDate;
}
