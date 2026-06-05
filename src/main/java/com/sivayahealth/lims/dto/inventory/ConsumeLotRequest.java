package com.sivayahealth.lims.dto.inventory;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ConsumeLotRequest {
    private BigDecimal quantity;
    private String reason;
    private String refEntity;
    private Long refId;
}
