package com.neodent.auth.dto.response;

public record ValidatePasswordResetResponse(
    boolean valid,
    String message
) {}