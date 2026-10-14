package com.neodent.notification.controller;

import com.neodent.notification.dto.ActualizarConfiguracionNotificacionRequest;
import com.neodent.notification.dto.ConfiguracionNotificacionResponse;
import com.neodent.notification.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configuracion/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Configuración de Notificaciones", description = "Centro de configuración administrativa de notificaciones y recordatorios")
public class ConfiguracionNotificacionController {

    private final NotificacionService notificacionService;
    private final com.neodent.notification.service.EmailService emailService;

    @Operation(summary = "Listar todas las configuraciones de notificaciones")
    @GetMapping
    public ResponseEntity<List<ConfiguracionNotificacionResponse>> listarConfiguraciones() {
        return ResponseEntity.ok(notificacionService.obtenerConfiguraciones());
    }

    @Operation(summary = "Actualizar configuración de notificación por código")
    @PutMapping("/{codigo}")
    public ResponseEntity<ConfiguracionNotificacionResponse> actualizarConfiguracion(
        @PathVariable String codigo,
        @Valid @RequestBody ActualizarConfiguracionNotificacionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(notificacionService.actualizarConfiguracion(codigo, request, usuarioId));
    }

    @Operation(summary = "Enviar correo de prueba de plantilla de notificación")
    @PostMapping("/test")
    public ResponseEntity<java.util.Map<String, Object>> probarNotificacion(
        @Valid @RequestBody com.neodent.notification.dto.ProbarNotificacionRequest request
    ) {
        if ("CANCELACION_CITA".equalsIgnoreCase(request.tipo())) {
            emailService.enviarCitaCancelada(
                request.correoDestino(),
                new com.neodent.notification.dto.CitaCanceladaEmailData(
                    "Paciente de Demostración",
                    "20/10/2026",
                    "10:30 AM",
                    "Dr. Carlos Morales V.",
                    "Ortodoncia y Estética",
                    "Sede Central Miraflores",
                    "Prueba de envío manual desde el panel de administración"
                )
            );
        } else {
            emailService.enviarRecordatorioCita(
                request.correoDestino(),
                new com.neodent.notification.dto.CitaRecordatorioEmailData(
                    "Paciente de Demostración",
                    "20/10/2026",
                    "10:30 AM",
                    "Dr. Carlos Morales V.",
                    "Ortodoncia y Estética",
                    "Sede Central Miraflores",
                    "https://neodent.com/paciente/citas"
                )
            );
        }

        return ResponseEntity.ok(java.util.Map.of(
            "mensaje", "Prueba enviada con éxito a " + request.correoDestino(),
            "destinatario", request.correoDestino(),
            "tipo", request.tipo()
        ));
    }
}
