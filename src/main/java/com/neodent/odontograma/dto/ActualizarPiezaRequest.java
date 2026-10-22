package com.neodent.odontograma.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ActualizarPiezaRequest(
    @NotNull(message = "El número de pieza dental es obligatorio")
    Integer numeroPieza,

    @Size(max = 30, message = "La superficie no puede superar los 30 caracteres")
    String superficie,

    Integer idCondicion,

    String codigoCondicion,

    @Size(max = 255, message = "La observación no puede superar los 255 caracteres")
    String observacion,

    String estado,

    Long citaId
) {}
