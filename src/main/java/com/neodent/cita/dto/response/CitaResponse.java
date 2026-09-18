package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record CitaResponse(

    Long idCita,
    Long pacienteId,
    Long odontologoEspecialidadId,
    Integer sedeId,
    Integer servicioId,
    String estado,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    String message

) {}