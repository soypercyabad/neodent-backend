package com.neodent.auth.dto.response;

public record CompleteAccountActivationResponse(
    Long usuarioId,
    Long pacienteId,
    String message
) {}