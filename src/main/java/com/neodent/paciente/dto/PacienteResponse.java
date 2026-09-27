package com.neodent.paciente.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PacienteResponse(
    Long id,
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    LocalDate fechaNacimiento,
    String telefono,
    String email,
    String direccion,
    Boolean activo,
    Long usuarioId,
    boolean tieneCuenta,
    LocalDateTime fechaCreacion,
    LocalDateTime fechaActualizacion
) {}