package com.sivayahealth.lims.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    private String newPassword;
}
