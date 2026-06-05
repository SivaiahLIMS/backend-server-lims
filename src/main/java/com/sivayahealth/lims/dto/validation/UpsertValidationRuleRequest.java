package com.sivayahealth.lims.dto.validation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class UpsertValidationRuleRequest {

    private String fieldType;

    @Schema(description = "RANGE | MIN_ONLY | MAX_ONLY | TARGET_VARIANCE | FORMULA_BASED",
            example = "RANGE", allowableValues = {"RANGE","MIN_ONLY","MAX_ONLY","TARGET_VARIANCE","FORMULA_BASED"})
    private String ruleType = "RANGE";

    private String unit;
    private BigDecimal oosLowerLimit;
    private BigDecimal oosUpperLimit;
    private BigDecimal ootLowerLimit;
    private BigDecimal ootUpperLimit;

    private boolean requireCommentOnOos = true;
    private boolean requireCommentOnOot = true;
    private boolean requireReviewOnOot = false;
    private boolean requireInvestigationOnOos = false;
    private boolean requireCapaOnOos = false;

    private boolean active = true;
}
