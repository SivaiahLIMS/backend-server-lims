package com.sivayahealth.lims.dto.doccontrol;

import lombok.Data;

@Data
public class CreateDocControlRequest {
    private String title;
    private String docType;
    private String category;
    private Integer reviewPeriodMonths;
}
