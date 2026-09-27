package com.neodent.usuario.dto.response;

public record RolResponse(
    Integer id,
    String nombre,
    String descripcion,
    Boolean activo,
    boolean sistema
) {}