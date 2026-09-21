package com.neodent.auth.service;

import com.neodent.auth.model.TokenAccion;
import com.neodent.auth.repository.TokenAccionRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class StaffInvitationService {

    private static final String TIPO = AppConstants.TiposTokenAccion.STAFF_INVITATION;

    private final TokenAccionRepository repository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public String generar(Usuario usuario) {
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado()))
            throw new ConflictException("La cuenta ya fue activada");

        var anteriores = repository.findAllByUsuarioIdAndTipoAndUsadoFalseAndRevocadoFalse(usuario.getId(), TIPO);
        anteriores.forEach(t -> {
            t.setRevocado(true);
            t.setFechaRevocacion(LocalDateTime.now(clock));
        });
        repository.saveAll(anteriores);

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        TokenAccion invitacion = new TokenAccion();
        invitacion.setUsuario(usuario);
        invitacion.setTipo(TIPO);
        invitacion.setHashToken(hash(token));
        invitacion.setFechaExpiracion(LocalDateTime.now(clock).plusHours(24));
        invitacion.setUsado(false);
        invitacion.setRevocado(false);
        repository.save(invitacion);

        return frontendUrl + "/activar-personal?token=" + token;
    }

    @Transactional(readOnly = true)
    public Usuario validar(String token) {
        if (token == null || token.isBlank())
            throw new UnauthorizedException("Invitación inválida");

        TokenAccion invitacion = repository
            .findByHashTokenAndTipoAndUsadoFalseAndRevocadoFalse(hash(token), TIPO)
            .orElseThrow(() -> new UnauthorizedException("Invitación inválida o utilizada"));

        if (!LocalDateTime.now(clock).isBefore(invitacion.getFechaExpiracion()))
            throw new UnauthorizedException("La invitación ha expirado");

        Usuario usuario = invitacion.getUsuario();

        if (usuario == null || Boolean.TRUE.equals(usuario.getCorreoVerificado())
            || !AppConstants.EstadosUsuario.PENDIENTE.equals(usuario.getEstado().getNombre()))
            throw new UnauthorizedException("La invitación ya no está disponible");

        return usuario;
    }

    @Transactional
    public void consumir(String token) {
        validar(token);

        TokenAccion invitacion = repository
            .findByHashTokenAndTipoAndUsadoFalseAndRevocadoFalse(hash(token), TIPO)
            .orElseThrow(() -> new UnauthorizedException("Invitación inválida"));

        invitacion.setUsado(true);
        invitacion.setFechaUso(LocalDateTime.now(clock));
        repository.save(invitacion);
    }

    private static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo procesar la invitación", e);
        }
    }
}