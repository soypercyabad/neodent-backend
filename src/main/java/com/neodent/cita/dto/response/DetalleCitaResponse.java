package com.neodent.cita.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DetalleCitaResponse(
    Long idCita,

    Long pacienteId,
    String pacienteNombre,
    String pacienteTipoDocumento,
    String pacienteNumeroDocumento,
    String pacienteTelefono,
    String pacienteCorreo,

    Long odontologoEspecialidadId,
    Long odontologoId,
    String odontologoNombre,
    String especialidadNombre,

    Integer sedeId,
    String sedeNombre,
    String sedeDireccion,

    Integer servicioId,
    String servicioNombre,
    String servicioDescripcion,
    Integer duracionMinutos,
    BigDecimal precioReferencial,

    String estado,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,

    String motivo,
    String observaciones,
    LocalDateTime confirmadaEn,

    String creadoPor,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion
) {}