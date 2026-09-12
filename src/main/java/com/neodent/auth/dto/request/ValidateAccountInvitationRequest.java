package com.neodent.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateAccountInvitationRequest(

    @NotBlank(
        message = "El token es obligatorio"
    )
    String token

) {}