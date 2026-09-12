package com.neodent.auth.dto.response;

public record VerifyTwoFactorResponse(

    boolean verified,
    String accessToken,
    String tokenType,
    long expiresIn,
    String message

) {}