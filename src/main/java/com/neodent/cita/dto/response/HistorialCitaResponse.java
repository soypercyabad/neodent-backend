package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record HistorialCitaResponse(
    Long id,
    String accion,
    String estadoAnterior,
    String estadoNuevo,
    LocalDateTime fechaHoraAnterior,
    LocalDateTime fechaHoraNueva,
    String motivo,
    String realizadoPor,
    LocalDateTime fechaCreacion
) {}