package com.neodent.cita.dto.response;

import java.time.LocalDateTime;

public record ReservaCitaResponse(
    String tokenReserva,
    LocalDateTime fechaHoraInicio,
    LocalDateTime fechaHoraFin,
    LocalDateTime expiresAt,
    long segundosRestantes,
    String message
) {}