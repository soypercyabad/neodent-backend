package com.neodent.auth.dto.response;

public record StartAccountActivationResponse(

    Long challengeId,
    String message

) {}