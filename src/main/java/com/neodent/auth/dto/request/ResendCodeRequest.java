package com.neodent.auth.dto.request;

import jakarta.validation.constraints.NotNull;

public record ResendCodeRequest(

    @NotNull(message = "El challengeId es obligatorio")
    Long challengeId

) {}