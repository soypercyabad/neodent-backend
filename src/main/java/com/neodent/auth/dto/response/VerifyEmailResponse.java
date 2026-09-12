package com.neodent.auth.dto.response;

public record VerifyEmailResponse(
    boolean verified,
    String message
) {}