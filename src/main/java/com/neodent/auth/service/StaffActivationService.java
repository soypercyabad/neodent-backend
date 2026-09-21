package com.neodent.auth.service;

import com.neodent.auth.dto.request.CompleteStaffActivationRequest;
import com.neodent.auth.dto.request.StartStaffActivationRequest;
import com.neodent.auth.dto.response.AccountInvitationResponse;
import com.neodent.auth.dto.response.CompleteAccountActivationResponse;
import com.neodent.auth.dto.response.StartAccountActivationResponse;
import com.neodent.personal.repository.PersonalRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.EstadoUsuarioRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaffActivationService {

    private final StaffInvitationService invitations;
    private final OtpService otpService;
    private final TurnstileService turnstileService;
    private final RegistrationRateLimitService rateLimitService;
    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final EstadoUsuarioRepository estadoRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public AccountInvitationResponse validar(String token) {
        Usuario usuario = invitations.validar(token);
        var personal = personalRepository.findByUsuarioId(usuario.getId())
            .orElseThrow(() -> new UnauthorizedException("Invitación de personal inválida"));

        return new AccountInvitationResponse(
            true,
            personal.getNombres() + " " + personal.getApellidoPaterno(),
            enmascarar(usuario.getCorreo())
        );
    }

    @Transactional
    public StartAccountActivationResponse iniciar(StartStaffActivationRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);

        Usuario usuario = invitations.validar(request.token());
        var personal = personalRepository.findByUsuarioId(usuario.getId())
            .orElseThrow(() -> new UnauthorizedException("Invitación de personal inválida"));

        boolean coincide = personal.getTipoDocumento().getCodigo()
            .equalsIgnoreCase(request.tipoDocumento().trim())
            && personal.getNumeroDocumento().equals(request.numeroDocumento().trim());

        if (!coincide)
            throw new UnauthorizedException("Los datos de identidad no coinciden");

        var otp = otpService.generarStaffActivationOtp(usuario);

        return new StartAccountActivationResponse(
            otp.id(),
            enmascarar(usuario.getCorreo()),
            "Te enviamos un código para activar tu cuenta"
        );
    }

    @Transactional
    public CompleteAccountActivationResponse completar(CompleteStaffActivationRequest request) {
        Usuario invitado = invitations.validar(request.token());
        Usuario verificado = otpService.verificarStaffActivationOtp(
            request.challengeId(), request.codigo()
        );

        if (!invitado.getId().equals(verificado.getId())) {
            throw new UnauthorizedException("El código no corresponde a esta invitación");
        }

        var activo = estadoRepository
            .findByNombreAndActivoTrue(AppConstants.EstadosUsuario.ACTIVO)
            .orElseThrow(() -> new IllegalStateException("Estado ACTIVO no configurado"));

        invitations.consumir(request.token());

        invitado.setHashContrasena(passwordEncoder.encode(request.password()));
        invitado.setCorreoVerificado(true);
        invitado.setEstado(activo);
        usuarioRepository.save(invitado);

        return new CompleteAccountActivationResponse(
            invitado.getId(),
            null,
            "Cuenta de personal activada correctamente"
        );
    }

    private static String enmascarar(String correo) {
        int at = correo.indexOf('@');
        return at <= 2
            ? "***" + correo.substring(at)
            : correo.substring(0, 2) + "***" + correo.substring(at);
    }
}