package com.neodent.dashboard.dto.response;

import java.util.List;

public record ReceptionDashboardResponse(
    int citasHoy,
    int citasPendientesHoy,
    int citasAtendidasHoy,
    int citasEnAtencionHoy,
    int citasPendientesTotal,
    int citasProximasTotal,
    long pacientesNuevosSemana,
    List<DashboardCitaResumen> citasDeHoy,
    List<DashboardCitaResumen> citasProximas
) {}
