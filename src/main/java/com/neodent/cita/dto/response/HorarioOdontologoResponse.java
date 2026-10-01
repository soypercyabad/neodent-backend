package com.neodent.cita.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;

public record HorarioOdontologoResponse(
    Long id,
    Long odontologoEspecialidadId,
    Integer sedeId,
    Byte diaSemana,
    LocalTime horaInicio,
    LocalTime horaFin,
    LocalDate fechaInicioVigencia,
    LocalDate fechaFinVigencia,
    Boolean activo
) {}
