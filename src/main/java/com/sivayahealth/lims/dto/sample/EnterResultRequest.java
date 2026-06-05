package com.sivayahealth.lims.dto.sample;

import com.sivayahealth.lims.entity.ResultQualifier;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class EnterResultRequest {
    private String parameterName;
    private String resultValue;
    private BigDecimal numericValue;
    private String unit;
    private ResultQualifier qualifier;
    private String remarks;
}
