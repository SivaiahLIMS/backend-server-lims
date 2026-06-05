package com.sivayahealth.lims.dto.doccontrol;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PublishVersionRequest {
    private LocalDate effectiveDate;
}
