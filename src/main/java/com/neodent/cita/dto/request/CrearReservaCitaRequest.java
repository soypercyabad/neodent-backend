package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CrearReservaCitaRequest(

    Long pacienteId,

    @NotNull
    Long odontologoEspecialidadId,

    @NotNull
    Integer sedeId,

    Integer servicioId,

    @NotNull
    LocalDateTime fechaHoraInicio

) {}