package com.neodent.dashboard.controller;

import com.neodent.dashboard.dto.response.AdminDashboardResponse;
import com.neodent.dashboard.dto.response.DentistDashboardResponse;
import com.neodent.dashboard.dto.response.ReceptionDashboardResponse;
import com.neodent.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Endpoints optimizados para tableros analíticos e indicadores")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "Obtiene los KPIs, estadísticas agregadas y citas recientes para el administrador")
    @GetMapping("/admin")
    public AdminDashboardResponse obtenerAdmin(
            @RequestParam(defaultValue = "30D") String periodo
    ) {
        return dashboardService.obtenerAdmin(periodo);
    }

    @Operation(summary = "Obtiene los KPIs y citas del odontólogo autenticado")
    @GetMapping("/odontologo")
    public DentistDashboardResponse obtenerOdontologo(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return dashboardService.obtenerOdontologo(usuarioId);
    }

    @Operation(summary = "Obtiene los KPIs y citas operativas del día para recepción")
    @GetMapping("/recepcion")
    public ReceptionDashboardResponse obtenerRecepcion() {
        return dashboardService.obtenerRecepcion();
    }
}
