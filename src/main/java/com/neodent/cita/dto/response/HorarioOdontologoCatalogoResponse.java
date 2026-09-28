package com.neodent.cita.dto.response;

public record HorarioOdontologoCatalogoResponse(
    Long odontologoEspecialidadId,
    Long odontologoId,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String numeroColegiatura,
    Integer especialidadId,
    String especialidadNombre,
    boolean tieneFoto
) {}