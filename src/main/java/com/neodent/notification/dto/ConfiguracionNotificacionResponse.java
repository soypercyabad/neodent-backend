package com.neodent.notification.dto;

import java.time.LocalDateTime;

public record ConfiguracionNotificacionResponse(
    Integer id,
    String codigo,
    String nombre,
    String descripcion,
    boolean canalCorreo,
    boolean canalPortal,
    int anticipacionValor,
    String anticipacionUnidad,
    boolean activo,
    String actualizadoPor,
    LocalDateTime fechaActualizacion
) {}
