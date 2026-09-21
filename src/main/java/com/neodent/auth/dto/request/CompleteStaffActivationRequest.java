package com.neodent.auth.dto.request;

import jakarta.validation.constraints.*;

public record CompleteStaffActivationRequest(
    @NotBlank String token,
    @NotNull Long challengeId,
    @NotBlank @Pattern(regexp = "\\d{6}") String codigo,
    @NotBlank @Size(min = 8, max = 100) String password
) {}