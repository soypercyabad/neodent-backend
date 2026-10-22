package com.neodent.odontograma.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OdontogramaResponse(
    Long id,
    Long historiaId,
    Long pacienteId,
    LocalDateTime fecha,
    String observaciones,
    Long odontologoId,
    String odontologoNombre,
    String numeroColegiatura,
    List<PiezaDentalDetalleDto> piezas
) {}
