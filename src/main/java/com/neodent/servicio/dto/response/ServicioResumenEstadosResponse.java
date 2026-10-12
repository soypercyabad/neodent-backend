package com.neodent.servicio.dto.response;

public record ServicioResumenEstadosResponse(
    long todos,
    long destacados,
    long activos,
    long inactivos
) {}
