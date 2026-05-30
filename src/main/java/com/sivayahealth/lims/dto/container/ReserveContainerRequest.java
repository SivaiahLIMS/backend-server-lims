package com.sivayahealth.lims.dto.container;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ReserveContainerRequest {
    private Long containerId;
    private BigDecimal reservedQty;
    private Long userId;
}
