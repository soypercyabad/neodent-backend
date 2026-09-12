package com.neodent.auth.service;

import com.neodent.auth.model.TokenAccion;
import com.neodent.auth.repository.TokenAccionRepository;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountInvitationService {

    private static final String TIPO =  AppConstants.TiposTokenAccion.ACCOUNT_INVITATION;
    private static final int EXPIRACION_HORAS = 24;
    private final TokenAccionRepository repository;
    private final SecureRandom random = new SecureRandom();
    private final PacienteRepository pacienteRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;


    @Transactional
    public AccountInvitation generar(
        Paciente paciente
    ) {

        if (paciente.getUsuario() != null) {
            throw new ConflictException(
                "El paciente ya tiene una cuenta asociada"
            );
        }

        if (
            paciente.getEmail() == null ||
            paciente.getEmail().isBlank()
        ) {
            throw new ConflictException(
                "El paciente no tiene un correo registrado"
            );
        }

        revocarInvitacionesAnteriores(
            paciente.getId()
        );

        String token = generarTokenSeguro();

        TokenAccion invitacion =
            new TokenAccion();

        invitacion.setPaciente(paciente);
        invitacion.setUsuario(null);
        invitacion.setTipo(TIPO);
        invitacion.setTokenHash(
            hashToken(token)
        );

        invitacion.setExpiresAt(
            LocalDateTime.now()
                .plusHours(EXPIRACION_HORAS)
        );

        invitacion.setUsado(false);
        invitacion.setRevocado(false);

        repository.save(invitacion);

        String url =
            frontendUrl
                + "/activate-account?token="
                + token;

        return new AccountInvitation(url);
    }


    @Transactional(readOnly = true)
    public Paciente validarToken(
        String token
    ) {

        TokenAccion invitacion =
            repository
                .findByTokenHashAndTipoAndUsadoFalseAndRevocadoFalse(
                    hashToken(token),
                    TIPO
                )
                .orElseThrow(() ->
                    new UnauthorizedException(
                        "La invitación no es válida"
                    )
                );

        if (
            LocalDateTime.now()
                .isAfter(
                    invitacion.getExpiresAt()
                )
        ) {
            throw new UnauthorizedException(
                "La invitación ha expirado"
            );
        }

        Paciente paciente =
            invitacion.getPaciente();

        if (paciente.getUsuario() != null) {
            throw new ConflictException(
                "El paciente ya tiene una cuenta activa"
            );
        }

        return paciente;
    }


    private void revocarInvitacionesAnteriores(
        Long pacienteId
    ) {

        List<TokenAccion> anteriores =
            repository
                .findAllByPacienteIdAndTipoAndUsadoFalseAndRevocadoFalse(
                    pacienteId,
                    TIPO
                );

        LocalDateTime ahora =
            LocalDateTime.now();

        anteriores.forEach(
            token -> {
                token.setRevocado(true);
                token.setRevokedAt(ahora);
            }
        );

        repository.saveAll(anteriores);
    }


    private String generarTokenSeguro() {

        byte[] bytes = new byte[32];

        random.nextBytes(bytes);

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(bytes);
    }


    private String hashToken(
        String token
    ) {

        try {

            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                digest.digest(
                    token.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(hash);

        } catch (Exception ex) {

            throw new IllegalStateException(
                "No se pudo procesar el token",
                ex
            );
        }
    }


    @Transactional
    public void marcarComoUsada(
        String token
    ) {

        TokenAccion invitacion =
            repository
                .findByTokenHashAndTipoAndUsadoFalseAndRevocadoFalse(
                    hashToken(token),
                    AppConstants.TiposTokenAccion.ACCOUNT_INVITATION
                )
                .orElseThrow(() ->
                    new UnauthorizedException(
                        "La invitación no es válida"
                    )
                );

        invitacion.setUsado(true);

        invitacion.setUsedAt(
            LocalDateTime.now()
        );

        repository.save(invitacion);
    }


    @Transactional
    public AccountInvitation generar(
        Long pacienteId
    ) {

        Paciente paciente =
            pacienteRepository.findById(pacienteId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Paciente no encontrado"
                    )
                );

        return generar(paciente);
    }
}