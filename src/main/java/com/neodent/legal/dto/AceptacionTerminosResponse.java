package com.neodent.legal.dto;

import java.time.LocalDateTime;

public record AceptacionTerminosResponse(
    Long id,
    Long usuarioId,
    String correo,
    String nombreCompleto,
    String numeroDocumento,
    String versionTerminos,
    String tituloTerminos,
    LocalDateTime aceptadoEn,
    String ip,
    String agenteUsuario
) {}
