package com.neodent.usuario.dto.response;

public record ModuloResponse(
    Integer id,
    String codigo,
    String nombre,
    String descripcion,
    String ruta,
    String icono,
    Integer orden,
    Integer idModuloPadre,
    String nombreModuloPadre,
    Boolean activo
) {}
