package com.neodent.legal.dto;

import java.time.LocalDateTime;

public record AceptacionTerminosResponse(
    Long id,
    String tipoDocumento,
    String numeroDocumento,
    String terminoFirmado,
    String version,
    String ip,
    String agenteUsuario,
    LocalDateTime fechaAceptacion
) {}
