package com.neodent.notification.dto;

public record CitaRecordatorioEmailData(
    String pacienteNombre,
    String fechaCita,
    String horaCita,
    String odontologoNombre,
    String especialidad,
    String sedeNombre,
    String portalUrl
) {}
