package com.neodent.notification.dto;

public record CitaCanceladaEmailData(
    String nombrePaciente,
    String fecha,
    String hora,
    String odontologo,
    String especialidad,
    String sede,
    String motivo,
    String ctaUrl
) {
    public CitaCanceladaEmailData(
        String nombrePaciente,
        String fecha,
        String hora,
        String odontologo,
        String especialidad,
        String sede,
        String motivo
    ) {
        this(nombrePaciente, fecha, hora, odontologo, especialidad, sede, motivo, null);
    }
}