package com.neodent.auth.dto.response;

import java.util.List;

public record AuthenticatedUserResponse(
    Long idUsuario,
    String correo,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    List<String> roles,
    String estado,
    Long idPersonal,
    Long idPaciente
) {}