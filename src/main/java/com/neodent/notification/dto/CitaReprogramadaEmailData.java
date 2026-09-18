package com.neodent.notification.dto;

public record CitaReprogramadaEmailData(
    String nombrePaciente,
    String fechaAnterior,
    String horaAnterior,
    String fechaNueva,
    String horaNueva,
    String odontologo,
    String especialidad,
    String sede,
    String motivo,
    String ctaUrl
) {}