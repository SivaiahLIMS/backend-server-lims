package com.sivayahealth.lims.dto.sample;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class EnterResultRequest {
    private String parameterName;
    private String resultValue;
    private BigDecimal numericValue;
    private String unit;
}
