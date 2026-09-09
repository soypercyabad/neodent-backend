package com.neodent.paciente.dto;

public record PacienteResponse(
    Long id,
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String telefono,
    String email,
    boolean tieneCuenta
) {
}