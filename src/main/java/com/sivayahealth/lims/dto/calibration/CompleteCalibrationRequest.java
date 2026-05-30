package com.sivayahealth.lims.dto.calibration;

import lombok.Data;

@Data
public class CompleteCalibrationRequest {
    private Long userId;
    private String readingJson;
}
