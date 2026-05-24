package com.sivayahealth.lims.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
    @NotNull Long tenantId,
    @NotBlank @Size(min = 4, max = 100) String username,
    @NotBlank @Size(min = 8) String password,
    @Email String email,
    String firstName,
    String lastName,
    String phone
) {}
