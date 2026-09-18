package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record CrearBloqueoHorarioRequest(
    @NotNull Long odontologoId,
    Integer sedeId,
    @NotNull LocalDateTime fechaInicio,
    @NotNull LocalDateTime fechaFin,
    @Size(max = 255) String motivo
) {}