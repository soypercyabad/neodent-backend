package com.neodent.auth.dto.response;

public record PatientRegistrationResponse(

    Long pacienteId,
    Long usuarioId,
    Long challengeId,
    String message

) {}