package com.sivayahealth.lims.dto.validation;

import io.swagger.v3.oas.annotations.media.Schema;
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

    @Schema(description = "UI must show a mandatory comment field before saving")
    private boolean requiresComment;

    @Schema(description = "OOT: send this result for supervisor review before proceeding")
    private boolean requiresReview;

    @Schema(description = "OOS: an investigation record must be opened")
    private boolean requiresInvestigation;

    @Schema(description = "OOS: a CAPA record must be opened")
    private boolean requiresCapa;
}
