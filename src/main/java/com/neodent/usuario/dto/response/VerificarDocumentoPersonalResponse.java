package com.neodent.usuario.dto.response;

public record VerificarDocumentoPersonalResponse(
    boolean disponible,
    boolean encontradoProveedor,
    boolean permitirIngresoManual,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    String message
) {}