package com.neodent.auth.dto.response;

public record RestartEmailVerificationResponse(

    Long challengeId,
    String message

) {}