package com.sivayahealth.lims.dto.calibration;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateCalibrationRequest {
    private Long instrumentId;
    private Long limitSetId;
    private Long createdById;
    private LocalDateTime scheduledAt;
}
