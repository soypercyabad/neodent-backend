package com.neodent.odontograma.dto;

import java.time.LocalDateTime;

public record PiezaDentalDetalleDto(
    Long id,
    Integer numeroPieza,
    String superficie,
    Integer idCondicion,
    String codigoCondicion,
    String nombreCondicion,
    String colorHexadecimal,
    String observacion,
    String estado,
    LocalDateTime fecha
) {}
