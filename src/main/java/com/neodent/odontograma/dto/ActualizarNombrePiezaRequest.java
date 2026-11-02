package com.neodent.odontograma.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarNombrePiezaRequest(
    @NotBlank(message = "El nombre de la pieza dental es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    String nombre
) {}
