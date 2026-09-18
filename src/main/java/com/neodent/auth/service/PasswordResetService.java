package com.neodent.auth.service;

import com.neodent.auth.model.TokenAccion;
import com.neodent.auth.repository.TokenAccionRepository;
import com.neodent.notification.EmailService;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
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
public class PasswordResetService {

    private static final String TIPO = AppConstants.TiposTokenAccion.PASSWORD_RESET;
    private static final int EXPIRACION_MINUTOS = 30;

    private final RefreshTokenService refreshTokenService;
    private final UsuarioRepository usuarioRepository;
    private final TokenAccionRepository tokenAccionRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Clock clock;

    private final SecureRandom random = new SecureRandom();

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public void solicitar(String correo) {
        String correoNormalizado = correo.trim().toLowerCase();

        usuarioRepository.findByCorreoIgnoreCase(correoNormalizado)
            .ifPresent(usuario -> generarToken(usuario));
    }

    @Transactional(readOnly = true)
    public void validar(String token) {
        obtenerTokenValido(token);
    }

    @Transactional
    public void restablecer(String token, String nuevaContrasena, String confirmarContrasena) {
        if (!nuevaContrasena.equals(confirmarContrasena)) {
            throw new BusinessException("Las contraseñas no coinciden");
        }

        TokenAccion tokenAccion = obtenerTokenValido(token);
        Usuario usuario = tokenAccion.getUsuario();

        if (usuario == null) {
            throw new UnauthorizedException("El enlace de restablecimiento no es válido");
        }

        if (passwordEncoder.matches(nuevaContrasena, usuario.getHashContrasena())) {
            throw new BusinessException("La nueva contraseña debe ser diferente a la actual");
        }

        usuario.setHashContrasena(passwordEncoder.encode(nuevaContrasena));
        usuarioRepository.save(usuario);
        refreshTokenService.revocarTodos(usuario.getId());

        LocalDateTime ahora = LocalDateTime.now(clock);

        tokenAccion.setUsado(true);
        tokenAccion.setFechaUso(ahora);
        tokenAccionRepository.save(tokenAccion);

        revocarTokensRestantes(usuario.getId(), tokenAccion.getId(), ahora);
    }

    private void generarToken(Usuario usuario) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<TokenAccion> anteriores =
            tokenAccionRepository.findAllByUsuarioIdAndTipoAndUsadoFalseAndRevocadoFalse(
                usuario.getId(),
                TIPO
            );

        anteriores.forEach(token -> {
            token.setRevocado(true);
            token.setFechaRevocacion(ahora);
        });

        tokenAccionRepository.saveAll(anteriores);

        String token = generarTokenSeguro();

        TokenAccion tokenAccion = new TokenAccion();
        tokenAccion.setUsuario(usuario);
        tokenAccion.setPaciente(null);
        tokenAccion.setTipo(TIPO);
        tokenAccion.setHashToken(hashToken(token));
        tokenAccion.setFechaExpiracion(ahora.plusMinutes(EXPIRACION_MINUTOS));
        tokenAccion.setUsado(false);
        tokenAccion.setRevocado(false);

        tokenAccionRepository.save(tokenAccion);

        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        emailService.enviarRestablecimientoContrasena(usuario.getCorreo(), resetUrl);
    }

    private TokenAccion obtenerTokenValido(String token) {
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("El enlace de restablecimiento no es válido");
        }

        TokenAccion tokenAccion = tokenAccionRepository
            .findByHashTokenAndTipoAndUsadoFalseAndRevocadoFalse(hashToken(token), TIPO)
            .orElseThrow(() -> new UnauthorizedException(
                "El enlace de restablecimiento no es válido"
            ));

        if (LocalDateTime.now(clock).isAfter(tokenAccion.getFechaExpiracion())) {
            throw new UnauthorizedException(
                "El enlace de restablecimiento ha expirado"
            );
        }

        return tokenAccion;
    }

    private void revocarTokensRestantes(Long usuarioId, Long tokenUsadoId, LocalDateTime ahora) {
        List<TokenAccion> tokens =
            tokenAccionRepository.findAllByUsuarioIdAndTipoAndUsadoFalseAndRevocadoFalse(
                usuarioId,
                TIPO
            );

        tokens.stream()
            .filter(token -> !token.getId().equals(tokenUsadoId))
            .forEach(token -> {
                token.setRevocado(true);
                token.setFechaRevocacion(ahora);
            });

        tokenAccionRepository.saveAll(tokens);
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
            throw new IllegalStateException("No se pudo procesar el token", ex);
        }
    }
}