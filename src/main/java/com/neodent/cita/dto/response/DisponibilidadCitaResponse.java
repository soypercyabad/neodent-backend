package com.neodent.cita.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DisponibilidadCitaResponse(

    Long odontologoEspecialidadId,
    Integer sedeId,
    Integer servicioId,
    LocalDate fecha,
    Integer duracionMinutos,
    List<HorarioDisponibleResponse> horarios

) {}