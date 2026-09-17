package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelarCitaRequest(

    @NotBlank
    @Size(max = 255)
    String motivo

) {}