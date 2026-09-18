package com.neodent.usuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerificarDocumentoPersonalRequest(
    @NotNull Integer tipoDocumentoId,
    @NotBlank String numeroDocumento
) {}