package com.neodent.cita.dto.response;

import java.time.LocalTime;

public record HorarioDisponibleResponse(
    LocalTime horaInicio,
    LocalTime horaFin
) {}