package com.neodent.usuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RolRequest(
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 30, message = "El nombre del rol no puede superar 30 caracteres")
    String nombre,

    @Size(max = 120, message = "La descripción no puede superar 120 caracteres")
    String descripcion
) {}