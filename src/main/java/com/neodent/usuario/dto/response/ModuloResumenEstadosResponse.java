package com.neodent.usuario.dto.response;

public record ModuloResumenEstadosResponse(
    long todos,
    long activos,
    long inactivos
) {}
