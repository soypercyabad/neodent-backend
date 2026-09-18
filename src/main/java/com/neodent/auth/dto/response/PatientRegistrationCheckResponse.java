package com.neodent.auth.dto.response;

public record PatientRegistrationCheckResponse(

    String dni,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno,
    boolean manualEntryRequired

) {}