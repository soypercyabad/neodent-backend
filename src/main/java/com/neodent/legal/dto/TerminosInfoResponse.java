package com.neodent.legal.dto;

import java.time.LocalDateTime;

public record TerminosInfoResponse(
    boolean existe,
    String nombreArchivo,
    String claveS3,
    Long tamanoBytes,
    String contentType,
    String urlDescarga,
    LocalDateTime fechaActualizacion
) {}
