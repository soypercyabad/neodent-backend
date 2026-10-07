package com.neodent.auth.dto.response;

public record PatientRegistrationValidationResponse(
    boolean valid,
    String message
) {}
