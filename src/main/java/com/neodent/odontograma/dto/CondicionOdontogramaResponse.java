package com.neodent.odontograma.dto;

public record CondicionOdontogramaResponse(
    Integer id,
    String codigo,
    String nombre,
    String descripcion,
    String colorHexadecimal,
    Boolean activo,
    String aplicaA
) {}
