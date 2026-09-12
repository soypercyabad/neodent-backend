package com.neodent.auth.dto.response;

public record StartAccountActivationResponse(

    Long challengeId,
    String emailMasked,
    String message

) {}