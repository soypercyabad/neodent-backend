package com.neodent.dashboard.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
    DashboardKpis kpis,
    List<EvolucionItem> evolucion,
    List<TopServicioItem> topServicios,
    List<EstadoDistribucionItem> distribucionEstados,
    List<DoctorProductividadItem> productividadDoctores,
    List<SedeDemandaItem> sedesData,
    List<DashboardCitaResumen> citasRecientes
) {
    public record DashboardKpis(
        long pacientesTotal,
        long pacientesActivos,
        long pacientesNuevosMes,
        long citasTotal,
        long citasAtendidas,
        long citasConfirmadas,
        long citasCanceladas,
        long citasEnAtencion,
        long citasHoy,
        BigDecimal facturacionEstimada,
        long personalTotal,
        long odontologosTotal,
        long recepcionistasTotal,
        long sedesTotal,
        long serviciosTotal
    ) {}

    public record EvolucionItem(
        String fecha,
        @JsonProperty("Programadas") int programadas,
        @JsonProperty("Atendidas") int atendidas
    ) {}

    public record TopServicioItem(
        String name,
        int total
    ) {}

    public record EstadoDistribucionItem(
        String name,
        int value,
        String color
    ) {}

    public record DoctorProductividadItem(
        String doctor,
        int citas,
        int atendidas
    ) {}

    public record SedeDemandaItem(
        String name,
        int total
    ) {}
}
