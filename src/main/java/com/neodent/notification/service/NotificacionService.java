package com.neodent.notification.service;

import com.neodent.notification.dto.*;
import com.neodent.notification.model.ConfiguracionNotificacion;
import com.neodent.notification.model.TipoNotificacion;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface NotificacionService {

    void crearNotificacion(
        Long usuarioId,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        String enlace,
        String referenciaTipo,
        Long referenciaId
    );

    NotificacionesResumenResponse obtenerNotificacionesUsuario(Long usuarioId, Pageable pageable);

    NotificacionResponse marcarComoLeida(Long notificacionId, Long usuarioId);

    void marcarTodasComoLeidas(Long usuarioId);

    List<ConfiguracionNotificacionResponse> obtenerConfiguraciones();

    ConfiguracionNotificacionResponse actualizarConfiguracion(
        String codigo,
        ActualizarConfiguracionNotificacionRequest request,
        Long usuarioId
    );

    Optional<ConfiguracionNotificacion> obtenerConfiguracion(String codigo);
}
