package com.neodent.sede.dto.response;

public record SedeResumenEstadosResponse(
    long todas,
    long activas,
    long inactivas
) {}
