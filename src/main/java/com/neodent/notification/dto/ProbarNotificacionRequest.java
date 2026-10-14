package com.neodent.notification.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProbarNotificacionRequest(
    @NotBlank(message = "El tipo de notificación es obligatorio")
    String tipo,

    @NotBlank(message = "El correo de prueba es obligatorio")
    @Email(message = "El formato del correo electrónico no es válido")
    String correoDestino
) {}
