package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record TipoBloqueoHorarioResponse(
    Integer id,
    String codigo,
    String nombre,
    String descripcion,
    Boolean requiereOdontologo,
    Boolean requiereSede,
    Boolean permiteOdontologo,
    Boolean permiteSede,
    Boolean activo,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion
) {}