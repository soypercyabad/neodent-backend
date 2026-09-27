package com.neodent.paciente.dto;

public record TipoDocumentoResponse(
    Integer id,
    String codigo,
    String nombre,
    Byte longitudMin,
    Byte longitudMax
) {}