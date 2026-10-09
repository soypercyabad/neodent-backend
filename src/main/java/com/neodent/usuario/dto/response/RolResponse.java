package com.neodent.usuario.dto.response;

import java.util.List;

public record RolResponse(
    Integer id,
    String nombre,
    String descripcion,
    Boolean activo,
    boolean sistema,
    List<ModuloResponse> modulos
) {}