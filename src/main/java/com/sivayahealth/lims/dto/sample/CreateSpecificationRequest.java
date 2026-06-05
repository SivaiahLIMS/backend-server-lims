package com.sivayahealth.lims.dto.sample;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateSpecificationRequest {
    private Long productId;
    private Long testMethodId;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private BigDecimal targetValue;
    private String unit;
    private BigDecimal ootLower;
    private BigDecimal ootUpper;
    private BigDecimal oosLower;
    private BigDecimal oosUpper;
}
