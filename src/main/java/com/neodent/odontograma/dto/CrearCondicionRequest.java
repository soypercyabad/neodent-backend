package com.neodent.odontograma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearCondicionRequest(
    @NotBlank(message = "El nombre de la condición es obligatorio")
    @Size(max = 80, message = "El nombre no puede superar los 80 caracteres")
    String nombre,

    @Size(max = 30, message = "El código no puede superar los 30 caracteres")
    String codigo,

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    String descripcion,

    @Size(max = 7, message = "El color hexadecimal debe ser válido (ej. #EF4444)")
    String colorHexadecimal
) {}
