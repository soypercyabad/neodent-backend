package com.neodent.notification.dto;

public record CitaCanceladaEmailData(
    String nombrePaciente,
    String fecha,
    String hora,
    String odontologo,
    String especialidad,
    String sede,
    String motivo
) {}