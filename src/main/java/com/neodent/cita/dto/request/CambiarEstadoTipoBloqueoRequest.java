package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoTipoBloqueoRequest(

    @NotNull(message = "El estado activo es obligatorio")
    Boolean activo
) {}
