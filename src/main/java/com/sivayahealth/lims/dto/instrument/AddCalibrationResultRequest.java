package com.sivayahealth.lims.dto.instrument;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AddCalibrationResultRequest {
    private Long templateId;
    private BigDecimal observation;
}
