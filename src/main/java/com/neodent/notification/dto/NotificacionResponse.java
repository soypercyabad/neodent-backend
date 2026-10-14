package com.neodent.notification.dto;

import java.time.LocalDateTime;

public record NotificacionResponse(
    Long id,
    String tipo,
    String titulo,
    String mensaje,
    String enlace,
    String referenciaTipo,
    Long referenciaId,
    boolean leida,
    LocalDateTime leidaEn,
    LocalDateTime fechaCreacion
) {}
