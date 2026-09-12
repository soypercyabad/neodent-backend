package com.neodent.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PatientRegistrationCheckRequest(

    @NotBlank(message = "El DNI es obligatorio")
    @Pattern(
        regexp = "\\d{8}",
        message = "El DNI debe contener 8 dígitos"
    )
    String dni,

    @NotBlank(message = "La verificación de seguridad es obligatoria")
    String turnstileToken

) {}