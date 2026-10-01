package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TipoBloqueoHorarioRequest(

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no puede superar los 30 caracteres")
    String codigo,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    String nombre,

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    String descripcion,

    @NotNull
    Boolean requiereOdontologo,

    @NotNull
    Boolean requiereSede,

    @NotNull
    Boolean permiteOdontologo,

    @NotNull
    Boolean permiteSede
) {}