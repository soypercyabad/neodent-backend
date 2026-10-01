package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record BloqueoHorarioResponse(
    Long id,
    Integer tipoBloqueoId,
    String tipoBloqueoCodigo,
    String tipoBloqueoNombre,
    Long odontologoId,
    Integer sedeId,
    LocalDateTime fechaInicio,
    LocalDateTime fechaFin,
    String motivo,
    Long usuarioId,
    LocalDateTime fechaCreacion
) {}