package com.neodent.dashboard.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DashboardCitaResumen(
    Long idCita,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    Long pacienteId,
    String pacienteNombre,
    String pacienteTipoDocumento,
    String pacienteNumeroDocumento,
    String pacienteTelefono,
    String servicioNombre,
    String odontologoNombre,
    String sedeNombre,
    String estado,
    BigDecimal precioReferencial
) {}
