package com.neodent.notification.dto;

public record CitaConfirmadaEmailData(
    String nombrePaciente,
    String fecha,
    String hora,
    String odontologo,
    String especialidad,
    String sede,
    String ctaTexto,
    String ctaUrl
) {}