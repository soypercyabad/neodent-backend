package com.neodent.historia.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record HistoriaClinicaResponse(
    Long id,
    Long pacienteId,
    String nombrePaciente,
    String tipoDocumento,
    String numeroDocumento,
    LocalDate fechaNacimiento,
    Integer edad,
    String antecedentes,
    String alergias,
    String observacionesGenerales,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion,
    boolean tieneOdontograma,
    int totalEvoluciones
) {}
