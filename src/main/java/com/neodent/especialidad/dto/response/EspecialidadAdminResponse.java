package com.neodent.especialidad.dto.response;

public record EspecialidadAdminResponse(
    Integer id,
    String nombre,
    String descripcion,
    Boolean activo
) {}