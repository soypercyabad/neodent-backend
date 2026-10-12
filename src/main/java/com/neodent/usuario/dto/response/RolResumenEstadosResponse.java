package com.neodent.usuario.dto.response;

public record RolResumenEstadosResponse(
    long todos,
    long activos,
    long inactivos,
    long sistema,
    long personalizados
) {}
