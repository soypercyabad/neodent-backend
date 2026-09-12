package com.neodent.auth.dto.response;

public record AccountInvitationResponse(

    boolean valid,
    String nombrePaciente,
    String emailMasked

) {}