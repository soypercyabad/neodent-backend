package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record CitaResumenResponse(
    Long idCita,
    Long pacienteId,
    String pacienteNombre,
    String pacienteTelefono,
    String pacienteNumeroDocumento,
    String odontologoNombre,
    String especialidadNombre,
    String sedeNombre,
    String servicioNombre,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    Integer duracionMinutos,
    String estado
) {
}
