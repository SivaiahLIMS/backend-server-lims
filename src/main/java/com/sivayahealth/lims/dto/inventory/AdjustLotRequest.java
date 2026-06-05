package com.sivayahealth.lims.dto.inventory;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AdjustLotRequest {
    private BigDecimal quantity;
    private String movementType;
    private String reason;
}
