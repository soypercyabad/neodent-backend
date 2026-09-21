package com.neodent.usuario.dto.request;

import jakarta.validation.constraints.*;

import java.util.Set;

public record CrearUsuarioInternoRequest(
    @NotBlank @Email String correo,
    @NotEmpty Set<String> roles,

    @NotNull Integer tipoDocumentoId,
    @NotBlank @Size(max = 20) String numeroDocumento,
    @NotBlank @Size(max = 80) String nombres,
    @NotBlank @Size(max = 60) String apellidoPaterno,
    @Size(max = 60) String apellidoMaterno,
    @Size(max = 20) String telefono,

    @Size(max = 30) String numeroColegiatura,
    Set<Integer> especialidadIds
) {}