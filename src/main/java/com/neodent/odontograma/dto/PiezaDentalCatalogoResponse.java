package com.neodent.odontograma.dto;

public record PiezaDentalCatalogoResponse(
    Integer numeroPieza,
    String nombre,
    String tipoDenticion,
    Integer cuadrante,
    Boolean activo
) {}
