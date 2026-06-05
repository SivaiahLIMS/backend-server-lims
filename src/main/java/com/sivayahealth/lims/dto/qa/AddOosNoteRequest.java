package com.sivayahealth.lims.dto.qa;

import lombok.Data;

@Data
public class AddOosNoteRequest {
    private String noteType;
    private String text;
}
