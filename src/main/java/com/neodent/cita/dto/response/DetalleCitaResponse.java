package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record DetalleCitaResponse(
    Long idCita,
    Long pacienteId,
    String pacienteNombre,
    Long odontologoEspecialidadId,
    String odontologoNombre,
    String especialidadNombre,
    Integer sedeId,
    String sedeNombre,
    String sedeDireccion,
    Integer servicioId,
    String servicioNombre,
    String estado,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin
) {}