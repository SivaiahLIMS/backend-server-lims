package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddExecutionDataRequest {
    private Long fieldId;
    private String fieldName;
    private BigDecimal value;
    private String unit;
    private Long chemicalId;
    private Long instrumentId;
    private String comment;
    private String reason;
}
