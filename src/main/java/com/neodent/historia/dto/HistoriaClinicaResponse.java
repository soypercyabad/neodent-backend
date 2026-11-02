package com.neodent.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "Respuesta detallada de historia clínica del paciente")
public record HistoriaClinicaResponse(
    Long id,
    Long pacienteId,
    String nombrePaciente,
    String tipoDocumento,
    String numeroDocumento,
    LocalDate fechaNacimiento,
    Integer edad,
    String motivoConsulta,
    String enfermedadActual,
    String antecedentes,
    String alergias,
    String antecedentesMedicos,
    String medicamentosActuales,
    String habitos,
    String antecedentesOdontologicos,
    String examenExtraoral,
    String examenIntraoral,
    String diagnostico,
    String planTratamiento,
    String observacionesGenerales,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion,
    boolean tieneOdontograma,
    int totalEvoluciones
) {}
