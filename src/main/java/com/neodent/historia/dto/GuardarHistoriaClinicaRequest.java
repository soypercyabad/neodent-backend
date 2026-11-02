package com.neodent.historia.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar la ficha de historia clínica")
public record GuardarHistoriaClinicaRequest(
    @Size(max = 2000, message = "El motivo de consulta no puede superar los 2000 caracteres")
    String motivoConsulta,

    @Size(max = 5000, message = "La enfermedad actual no puede superar los 5000 caracteres")
    String enfermedadActual,

    @Size(max = 5000, message = "Los antecedentes no pueden superar los 5000 caracteres")
    String antecedentes,

    @Size(max = 2000, message = "Las alergias no pueden superar los 2000 caracteres")
    String alergias,

    @Size(max = 5000, message = "Los antecedentes médicos no pueden superar los 5000 caracteres")
    String antecedentesMedicos,

    @Size(max = 3000, message = "Los medicamentos actuales no pueden superar los 3000 caracteres")
    String medicamentosActuales,

    @Size(max = 3000, message = "Los hábitos no pueden superar los 3000 caracteres")
    String habitos,

    @Size(max = 5000, message = "Los antecedentes odontológicos no pueden superar los 5000 caracteres")
    String antecedentesOdontologicos,

    @Size(max = 3000, message = "El examen extraoral no puede superar los 3000 caracteres")
    String examenExtraoral,

    @Size(max = 3000, message = "El examen intraoral no puede superar los 3000 caracteres")
    String examenIntraoral,

    @Size(max = 5000, message = "El diagnóstico no puede superar los 5000 caracteres")
    String diagnostico,

    @Size(max = 5000, message = "El plan de tratamiento no puede superar los 5000 caracteres")
    String planTratamiento,

    @Size(max = 5000, message = "Las observaciones generales no pueden superar los 5000 caracteres")
    String observacionesGenerales
) {}
