package com.sivayahealth.lims.dto.validation;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpsertValidationRuleRequest {
    private String fieldType;
    private String unit;
    private BigDecimal oosLowerLimit;
    private BigDecimal oosUpperLimit;
    private BigDecimal ootLowerLimit;
    private BigDecimal ootUpperLimit;
    private boolean requireCommentOnOos = true;
    private boolean requireCommentOnOot = true;
    private boolean active = true;
}
