package com.neodent.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record StartAccountActivationRequest(

    @NotBlank
    String token,

    @NotBlank
    String tipoDocumento,

    @NotBlank
    String numeroDocumento,

    @NotBlank
    String turnstileToken

) {}