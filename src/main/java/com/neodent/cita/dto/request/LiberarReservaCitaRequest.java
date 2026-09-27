package com.neodent.cita.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LiberarReservaCitaRequest {

    @NotBlank(message = "El token de reserva es obligatorio")
    private String tokenReserva;

    public LiberarReservaCitaRequest() {}

    public LiberarReservaCitaRequest(String tokenReserva) {
        this.tokenReserva = tokenReserva;
    }

    public String getTokenReserva() { return tokenReserva; }

    public void setTokenReserva(String tokenReserva) {
        this.tokenReserva = tokenReserva;
    }
}