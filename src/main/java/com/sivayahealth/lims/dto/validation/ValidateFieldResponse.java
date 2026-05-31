package com.sivayahealth.lims.dto.validation;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ValidateFieldResponse {
    /** PASS | OOT | OOS | NO_RULE */
    private String status;
    private boolean oos;
    private boolean oot;
    /** HIGH (OOS) | MEDIUM (OOT) | LOW (PASS) | NONE (NO_RULE) */
    private String severity;
    private String message;
    private boolean requiresComment;
}
