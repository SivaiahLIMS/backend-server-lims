package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SaveFieldValueRequest {
    private BigDecimal numericValue;
    /** ml / L / g / kg / mg / µg / mEq / IU / % */
    private String unit;
    /** EXACT / APPROX / TRACE / ND */
    private String qualifier;
    private String comment;
}
