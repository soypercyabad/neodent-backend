package com.neodent.cita.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CrearHorarioOdontologoRequest(
    @NotNull Long odontologoEspecialidadId,
    @NotNull Integer sedeId,
    @NotNull @Min(1) @Max(7) Byte diaSemana,
    @NotNull LocalTime horaInicio,
    @NotNull LocalTime horaFin
) {}