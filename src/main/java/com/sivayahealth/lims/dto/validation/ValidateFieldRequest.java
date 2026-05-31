package com.sivayahealth.lims.dto.validation;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ValidateFieldRequest {
    private BigDecimal value;
    private String unit;
}
