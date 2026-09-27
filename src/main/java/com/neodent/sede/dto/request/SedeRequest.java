package com.neodent.sede.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SedeRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    String nombre,

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
    String direccion,

    @Size(max = 80) String distrito,
    @Size(max = 80) String provincia,
    @Size(max = 80) String departamento,
    @Size(max = 20) String telefono,

    @Email(message = "Ingresa un correo electrónico válido")
    @Size(max = 120) String email
) {}