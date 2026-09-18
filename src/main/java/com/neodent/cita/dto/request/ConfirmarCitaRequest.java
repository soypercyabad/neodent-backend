package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ConfirmarCitaRequest(

    @NotBlank
    String tokenReserva

) {}