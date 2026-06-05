package com.sivayahealth.lims.dto.sample;

import lombok.Data;

@Data
public class StartExecutionRequest {
    private Long instrumentId;
    private String comments;
}
