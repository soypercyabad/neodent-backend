package com.neodent.sede.dto.response;

public record SedeCatalogoResponse(
    Integer id,
    String nombre,
    String distrito,
    Boolean activo
) {}
