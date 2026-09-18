package com.neodent.auth.service;

import com.neodent.auth.model.TokenRefresco;
import com.neodent.auth.repository.TokenRefrescoRepository;
import com.neodent.auth.security.JwtService;
import com.neodent.shared.constants.AppConstants;
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
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final TokenRefrescoRepository tokenRefrescoRepository;
    private final JwtService jwtService;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional
    public RefreshGenerado crear(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        String token = generarTokenSeguro();

        TokenRefresco refresh = new TokenRefresco();
        refresh.setUsuario(usuario);
        refresh.setHashToken(hashToken(token));
        refresh.setFechaExpiracion(ahora.plusDays(refreshExpirationDays));
        refresh.setRevocado(false);

        tokenRefrescoRepository.save(refresh);

        return new RefreshGenerado(
            token,
            refreshExpirationDays * 24 * 60 * 60
        );
    }

    @Transactional
    public SesionRenovada renovar(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("La sesión no es válida");
        }

        TokenRefresco actual = tokenRefrescoRepository
            .findByHashToken(hashToken(token))
            .orElseThrow(() -> new UnauthorizedException("La sesión no es válida"));

        LocalDateTime ahora = LocalDateTime.now(clock);

        if (Boolean.TRUE.equals(actual.getRevocado())) {
            throw new UnauthorizedException("La sesión fue revocada");
        }

        if (!actual.getFechaExpiracion().isAfter(ahora)) {
            actual.setRevocado(true);
            actual.setFechaRevocacion(ahora);
            throw new UnauthorizedException("La sesión ha expirado");
        }

        Usuario usuario = actual.getUsuario();

        if (!AppConstants.EstadosUsuario.ACTIVO.equalsIgnoreCase(usuario.getEstado().getNombre())) {
            actual.setRevocado(true);
            actual.setFechaRevocacion(ahora);
            throw new UnauthorizedException("La cuenta no se encuentra activa");
        }

        actual.setRevocado(true);
        actual.setFechaRevocacion(ahora);

        RefreshGenerado nuevoRefresh = crear(usuario);
        String nuevoAccessToken = jwtService.generarAccessToken(usuario);

        return new SesionRenovada(
            nuevoAccessToken,
            nuevoRefresh.token(),
            nuevoRefresh.maxAgeSeconds()
        );
    }

    @Transactional
    public void revocar(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        tokenRefrescoRepository.findByHashToken(hashToken(token))
            .ifPresent(refresh -> {
                if (!Boolean.TRUE.equals(refresh.getRevocado())) {
                    refresh.setRevocado(true);
                    refresh.setFechaRevocacion(LocalDateTime.now(clock));
                }
            });
    }

    @Transactional
    public void revocarTodos(Long usuarioId) {
        List<TokenRefresco> tokens =
            tokenRefrescoRepository.findAllByUsuarioIdAndRevocadoFalse(usuarioId);

        LocalDateTime ahora = LocalDateTime.now(clock);

        tokens.forEach(token -> {
            token.setRevocado(true);
            token.setFechaRevocacion(ahora);
        });
    }

    private String generarTokenSeguro() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo procesar el refresh token", ex);
        }
    }

    public record RefreshGenerado(
        String token,
        long maxAgeSeconds
    ) {}

    public record SesionRenovada(
        String accessToken,
        String refreshToken,
        long refreshMaxAgeSeconds
    ) {}
}