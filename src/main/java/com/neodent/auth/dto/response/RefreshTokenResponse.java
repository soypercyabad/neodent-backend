package com.neodent.auth.dto.response;

public record RefreshTokenResponse(
    String accessToken,
    String tokenType,
    long expiresIn,
    String message
) {}