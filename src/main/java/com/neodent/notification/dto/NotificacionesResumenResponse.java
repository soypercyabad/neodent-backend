package com.neodent.notification.dto;

import java.util.List;

public record NotificacionesResumenResponse(
    List<NotificacionResponse> notificaciones,
    long noLeidasTotal,
    int totalPaginas,
    long totalElementos
) {}
