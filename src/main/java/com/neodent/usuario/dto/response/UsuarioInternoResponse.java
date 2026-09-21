package com.neodent.usuario.dto.response;

import java.util.List;

public record UsuarioInternoResponse(
    Long usuarioId,
    String aliasInterno,
    String correo,
    String estado,
    List<String> roles,

    Long personalId,
    Integer tipoDocumentoId,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String telefono,
    Boolean personalActivo,

    Long odontologoId,
    String numeroColegiatura,
    List<Integer> especialidadIds,
    List<String> especialidades
) {}