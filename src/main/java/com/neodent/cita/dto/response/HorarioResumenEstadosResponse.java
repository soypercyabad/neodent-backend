package com.neodent.cita.dto.response;

public record HorarioResumenEstadosResponse(
    long todos,
    long vigentes,
    long proximos,
    long expirados
) {}
