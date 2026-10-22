package com.neodent.historia.dto;

import jakarta.validation.constraints.Size;

public record GuardarHistoriaClinicaRequest(
    @Size(max = 5000, message = "Los antecedentes no pueden superar los 5000 caracteres")
    String antecedentes,

    @Size(max = 2000, message = "Las alergias no pueden superar los 2000 caracteres")
    String alergias,

    @Size(max = 5000, message = "Las observaciones generales no pueden superar los 5000 caracteres")
    String observacionesGenerales
) {}
