package com.neodent.cita.dto.response;

import java.time.LocalTime;

public record HorarioOdontologoResponse(
    Long id,
    Long odontologoEspecialidadId,
    Integer sedeId,
    Byte diaSemana,
    LocalTime horaInicio,
    LocalTime horaFin,
    Boolean activo
) {}