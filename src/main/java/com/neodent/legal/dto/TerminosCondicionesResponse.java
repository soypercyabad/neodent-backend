package com.neodent.legal.dto;

import java.time.LocalDateTime;

public record TerminosCondicionesResponse(
    Long id,
    String codigoDocumento,
    String titulo,
    String version,
    String nombreArchivo,
    String claveS3,
    String texto,
    String linkTexto,
    String textoAgrupado,
    Long tamanoBytes,
    Boolean activo,
    String urlVisualizar,
    String urlS3,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion
) {}
