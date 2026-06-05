package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class AddDeviationNoteRequest {
    private String noteType;
    private String text;
}
