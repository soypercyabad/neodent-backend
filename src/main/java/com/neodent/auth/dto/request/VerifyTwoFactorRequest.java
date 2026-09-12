package com.neodent.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record VerifyTwoFactorRequest(

    @NotNull(message = "El challengeId es obligatorio")
    Long challengeId,

    @NotBlank(message = "El código es obligatorio")
    @Pattern(
        regexp = "\\d{6}",
        message = "El código debe contener 6 dígitos"
    )
    String codigo
) {}