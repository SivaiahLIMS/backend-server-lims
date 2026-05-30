package com.sivayahealth.lims.dto.worksheet;

import lombok.Data;

@Data
public class ReviewResultRequest {
    /** PASS or FAIL */
    private String passFail;
    private String comments;
}
