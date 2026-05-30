package com.sivayahealth.lims.dto.container;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ConvertReservationRequest {
    private Long userId;
    private BigDecimal consumedQty;
}
