package com.neodent.auth.dto.response;

public record PatientRegistrationCheckResponse(
    String tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    boolean manualEntryRequired
) {}