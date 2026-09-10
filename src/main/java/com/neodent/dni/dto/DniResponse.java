package com.neodent.dni.dto;

public record DniResponse(
    String dni,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno
) {
}