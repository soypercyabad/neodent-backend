package com.neodent.notification.dto;

import jakarta.validation.constraints.Min;

public record ActualizarConfiguracionNotificacionRequest(
    Boolean canalCorreo,
    Boolean canalPortal,
    @Min(value = 1, message = "La anticipación mínima es de 1")
    Integer anticipacionValor,
    String anticipacionUnidad,
    Boolean activo
) {}
