package com.neodent.legal.dto;

import java.time.LocalDateTime;

public record TerminosCondicionesResponse(
    Long id,
    String titulo,
    String version,
    String nombreArchivo,
    String claveS3,
    Long tamanoBytes,
    Boolean activo,
    String urlVisualizar,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion
) {}
