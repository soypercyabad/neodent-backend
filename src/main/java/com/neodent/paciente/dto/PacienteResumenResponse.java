package com.neodent.paciente.dto;

import com.neodent.paciente.model.OrigenRegistro;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
    name = "PacienteResumenResponse",
    description = "Resumen de datos del paciente para listados y tablas"
)
public record PacienteResumenResponse(
    Long id,
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String telefono,
    String email,
    Boolean activo,
    boolean tieneCuenta,
    String estadoCuenta,
    OrigenRegistro origenRegistro,
    String sedeRegistroNombre,
    LocalDateTime fechaCreacion
) {}
