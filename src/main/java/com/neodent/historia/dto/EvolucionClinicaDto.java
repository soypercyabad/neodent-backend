package com.neodent.historia.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class EvolucionClinicaDto {

    public record Request(
        Long citaId,

        @NotBlank(message = "El diagnóstico es obligatorio")
        String diagnostico,

        String procedimiento,
        String indicaciones,
        String observaciones
    ) {}

    public record Response(
        Long id,
        Long historiaId,
        Long citaId,
        Long odontologoId,
        String odontologoNombre,
        String numeroColegiatura,
        LocalDateTime fecha,
        String diagnostico,
        String procedimiento,
        String indicaciones,
        String observaciones
    ) {}
}
