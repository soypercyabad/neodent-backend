package com.neodent.notification.service;

import com.neodent.notification.dto.*;
import com.neodent.notification.model.ConfiguracionNotificacion;
import com.neodent.notification.model.Notificacion;
import com.neodent.notification.model.TipoNotificacion;
import com.neodent.notification.repository.ConfiguracionNotificacionRepository;
import com.neodent.notification.repository.NotificacionRepository;
import com.neodent.shared.exception.ForbiddenException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final ConfiguracionNotificacionRepository configuracionRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public void crearNotificacion(
        Long usuarioId,
        TipoNotificacion tipo,
        String titulo,
        String mensaje,
        String enlace,
        String referenciaTipo,
        Long referenciaId
    ) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElse(null);

        if (usuario == null) {
            log.warn("No se pudo crear notificación: usuario con ID {} no encontrado", usuarioId);
            return;
        }

        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setTipo(tipo);
        notificacion.setTitulo(titulo);
        notificacion.setMensaje(mensaje);
        notificacion.setEnlace(enlace);
        notificacion.setReferenciaTipo(referenciaTipo);
        notificacion.setReferenciaId(referenciaId);
        notificacion.setLeida(false);

        notificacionRepository.save(notificacion);
        log.info("Notificación creada exitosamente para usuario ID {}: {}", usuarioId, titulo);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificacionesResumenResponse obtenerNotificacionesUsuario(Long usuarioId, Pageable pageable) {
        Page<Notificacion> pagina = notificacionRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId, pageable);
        long noLeidasTotal = notificacionRepository.countByUsuarioIdAndLeidaFalse(usuarioId);

        List<NotificacionResponse> lista = pagina.getContent().stream()
            .map(this::mapearResponse)
            .toList();

        return new NotificacionesResumenResponse(
            lista,
            noLeidasTotal,
            pagina.getTotalPages(),
            pagina.getTotalElements()
        );
    }

    @Override
    @Transactional
    public NotificacionResponse marcarComoLeida(Long notificacionId, Long usuarioId) {
        Notificacion notif = notificacionRepository.findById(notificacionId)
            .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));

        if (!notif.getUsuario().getId().equals(usuarioId)) {
            throw new ForbiddenException("No tienes permiso para acceder a esta notificación");
        }

        if (!notif.isLeida()) {
            notif.setLeida(true);
            notif.setLeidaEn(LocalDateTime.now());
            notif = notificacionRepository.save(notif);
        }

        return mapearResponse(notif);
    }

    @Override
    @Transactional
    public void marcarTodasComoLeidas(Long usuarioId) {
        notificacionRepository.marcarTodasComoLeidas(usuarioId, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConfiguracionNotificacionResponse> obtenerConfiguraciones() {
        return configuracionRepository.findAll().stream()
            .map(this::mapearConfigResponse)
            .toList();
    }

    @Override
    @Transactional
    public ConfiguracionNotificacionResponse actualizarConfiguracion(
        String codigo,
        ActualizarConfiguracionNotificacionRequest request,
        Long usuarioId
    ) {
        ConfiguracionNotificacion config = configuracionRepository.findByCodigo(codigo)
            .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada para código: " + codigo));

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (request.canalCorreo() != null) {
            config.setCanalCorreo(request.canalCorreo());
        }
        if (request.canalPortal() != null) {
            config.setCanalPortal(request.canalPortal());
        }
        if (request.anticipacionValor() != null) {
            config.setAnticipacionValor(request.anticipacionValor());
        }
        if (request.anticipacionUnidad() != null && !request.anticipacionUnidad().isBlank()) {
            config.setAnticipacionUnidad(request.anticipacionUnidad().toUpperCase());
        }
        if (request.activo() != null) {
            config.setActivo(request.activo());
        }

        config.setActualizadoPor(usuario);
        config = configuracionRepository.save(config);

        return mapearConfigResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ConfiguracionNotificacion> obtenerConfiguracion(String codigo) {
        return configuracionRepository.findByCodigo(codigo);
    }

    private NotificacionResponse mapearResponse(Notificacion n) {
        return new NotificacionResponse(
            n.getId(),
            n.getTipo() != null ? n.getTipo().name() : "SISTEMA",
            n.getTitulo(),
            n.getMensaje(),
            n.getEnlace(),
            n.getReferenciaTipo(),
            n.getReferenciaId(),
            n.isLeida(),
            n.getLeidaEn(),
            n.getFechaCreacion()
        );
    }

    private ConfiguracionNotificacionResponse mapearConfigResponse(ConfiguracionNotificacion c) {
        String actualizadoPorNombre = null;
        if (c.getActualizadoPor() != null) {
            actualizadoPorNombre = c.getActualizadoPor().getCorreo();
        }

        return new ConfiguracionNotificacionResponse(
            c.getId(),
            c.getCodigo(),
            c.getNombre(),
            c.getDescripcion(),
            c.isCanalCorreo(),
            c.isCanalPortal(),
            c.getAnticipacionValor(),
            c.getAnticipacionUnidad(),
            c.isActivo(),
            actualizadoPorNombre,
            c.getFechaActualizacion()
        );
    }
}
