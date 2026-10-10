package com.neodent.dashboard.dto.response;

import java.util.List;

public record DentistDashboardResponse(
    int citasHoy,
    int citasAtendidasHoy,
    int citasProximas,
    int pacientesUnicos,
    List<DashboardCitaResumen> citas
) {}
