package com.neodent.notification.controller;

import com.neodent.notification.dto.NotificacionResponse;
import com.neodent.notification.dto.NotificacionesResumenResponse;
import com.neodent.notification.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Centro de notificaciones internas del usuario")
public class NotificacionController {

    private final NotificacionService notificacionService;

    @Operation(summary = "Obtener notificaciones del usuario autenticado")
    @GetMapping
    public ResponseEntity<NotificacionesResumenResponse> obtenerMisNotificaciones(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50));
        return ResponseEntity.ok(notificacionService.obtenerNotificacionesUsuario(usuarioId, pageable));
    }

    @Operation(summary = "Marcar notificación como leída")
    @PatchMapping("/{id}/leer")
    public ResponseEntity<NotificacionResponse> marcarComoLeida(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(notificacionService.marcarComoLeida(id, usuarioId));
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas")
    @PatchMapping("/marcar-todas-leidas")
    public ResponseEntity<Void> marcarTodasComoLeidas(
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        notificacionService.marcarTodasComoLeidas(usuarioId);
        return ResponseEntity.noContent().build();
    }
}
