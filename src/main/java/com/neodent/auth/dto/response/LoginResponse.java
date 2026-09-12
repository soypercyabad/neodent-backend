package com.neodent.auth.dto.response;

public record LoginResponse(

    boolean requiresTwoFactor,
    Long challengeId,
    String message

) {}