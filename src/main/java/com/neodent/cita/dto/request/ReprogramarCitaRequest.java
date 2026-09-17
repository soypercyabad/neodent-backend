package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ReprogramarCitaRequest(

    @NotNull
    LocalDateTime fechaHoraInicio,

    @Size(max = 255)
    String motivo

) {}