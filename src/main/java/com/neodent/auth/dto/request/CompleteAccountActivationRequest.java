package com.neodent.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteAccountActivationRequest(

    @NotBlank
    String token,

    @NotNull
    Long challengeId,

    @NotBlank
    @Pattern(regexp = "\\d{6}")
    String codigo,

    @NotBlank
    @Size(min = 8, max = 100)
    String password

) {}