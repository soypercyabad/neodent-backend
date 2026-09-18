package com.neodent.auth.dto.response;

public record ResendCodeResponse(
    Long challengeId,
    String message
) {}