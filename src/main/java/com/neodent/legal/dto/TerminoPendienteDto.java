package com.neodent.legal.dto;

public record TerminoPendienteDto(
    Long id,
    String codigo,
    String titulo,
    String version,
    String nombreArchivo,
    String texto,
    String linkTexto,
    String textoAgrupado,
    String urlVisualizar
) {}
