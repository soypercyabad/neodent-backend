package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrearBloqueoHorarioRequest(

    @NotNull(message = "El tipo de bloqueo es obligatorio")
    Integer tipoBloqueoId,

    Long odontologoId,
    Integer sedeId,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDateTime fechaInicio,

    @NotNull(message = "La fecha de fin es obligatoria")
    LocalDateTime fechaFin,

    @Size(max = 255, message = "El motivo no puede superar los 255 caracteres")
    String motivo
) {}