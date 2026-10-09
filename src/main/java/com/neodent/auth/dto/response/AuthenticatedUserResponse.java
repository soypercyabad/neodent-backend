package com.neodent.auth.dto.response;

import com.neodent.usuario.dto.response.ModuloResponse;

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
    Long idPaciente,
    List<ModuloResponse> modulos
) {}