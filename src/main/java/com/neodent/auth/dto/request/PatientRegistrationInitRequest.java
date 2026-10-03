package com.neodent.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PatientRegistrationInitRequest(
    @NotBlank @Size(max = 10)
    String tipoDocumento,

    @NotBlank @Size(max = 20)
    String numeroDocumento,

    @NotBlank @Email
    String email,

    @NotBlank
    String turnstileToken
) {}
