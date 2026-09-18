package com.neodent.auth.service;

import com.neodent.auth.dto.request.CompleteAccountActivationRequest;
import com.neodent.auth.dto.request.StartAccountActivationRequest;
import com.neodent.auth.dto.response.AccountInvitationResponse;
import com.neodent.auth.dto.response.CompleteAccountActivationResponse;
import com.neodent.auth.dto.response.StartAccountActivationResponse;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.EstadoUsuario;
import com.neodent.usuario.model.Rol;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.EstadoUsuarioRepository;
import com.neodent.usuario.repository.RolRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountActivationService {

    private final AccountInvitationService accountInvitationService;
    private final OtpService otpService;
    private final TurnstileService turnstileService;
    private final RegistrationRateLimitService rateLimitService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final PacienteRepository pacienteRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountInvitationResponse validar(String token) {
        Paciente paciente = accountInvitationService.validarToken(token);
        String nombre = paciente.getNombres() + " " + paciente.getApellidoPaterno();
        return new AccountInvitationResponse(true, nombre, enmascararEmail(paciente.getCorreo()));
    }

    @Transactional
    public StartAccountActivationResponse iniciar(StartAccountActivationRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);

        Paciente paciente = accountInvitationService.validarToken(request.token());

        String tipoDocumento = request.tipoDocumento().trim().toUpperCase();
        String numeroDocumento = request.numeroDocumento().trim();

        boolean documentoCoincide = paciente.getTipoDocumento().getCodigo().equalsIgnoreCase(tipoDocumento)
            && paciente.getNumeroDocumento().equals(numeroDocumento);

        if (!documentoCoincide) {
            throw new UnauthorizedException("Los datos de identidad no coinciden");
        }

        OtpService.OtpGenerado otp = otpService.generarAccountActivationOtp(paciente);
        return new StartAccountActivationResponse(
            otp.id(),
            enmascararEmail(paciente.getCorreo()),
            "Se envió un código para continuar con la activación"
        );
    }

    @Transactional
    public CompleteAccountActivationResponse completar(CompleteAccountActivationRequest request) {
        Paciente pacienteToken = accountInvitationService.validarToken(request.token());
        Paciente pacienteOtp = otpService.verificarAccountActivationOtp(request.challengeId(), request.codigo());

        if (!pacienteToken.getId().equals(pacienteOtp.getId())) {
            throw new UnauthorizedException("La verificación no corresponde a esta invitación");
        }

        if (pacienteToken.getUsuario() != null) {
            throw new ConflictException("El paciente ya tiene una cuenta asociada");
        }

        String email = pacienteToken.getCorreo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta registrada con este correo");
        }

        Rol rol = rolRepository.findByNombreAndActivoTrue(AppConstants.Roles.PACIENTE)
            .orElseThrow(() -> new IllegalStateException("Rol PACIENTE no configurado"));

        EstadoUsuario activo = estadoUsuarioRepository.findByNombreAndActivoTrue(AppConstants.EstadosUsuario.ACTIVO)
            .orElseThrow(() -> new IllegalStateException("Estado ACTIVO no configurado"));

        Usuario usuario = new Usuario();
        usuario.setAliasInterno(null);
        usuario.setCorreo(email);
        usuario.setHashContrasena(passwordEncoder.encode(request.password()));
        usuario.getRoles().add(rol);
        usuario.setEstado(activo);
        usuario.setCorreoVerificado(true);
        usuario.setSegundoFactor(true);

        Usuario guardado = usuarioRepository.save(usuario);
        pacienteToken.setUsuario(guardado);
        pacienteRepository.save(pacienteToken);

        accountInvitationService.marcarComoUsada(request.token());

        return new CompleteAccountActivationResponse(
            guardado.getId(),
            pacienteToken.getId(),
            "Cuenta activada correctamente"
        );
    }

    private String enmascararEmail(String correo) {
        int arroba = correo.indexOf("@");
        if (arroba <= 2) {
            return "***" + correo.substring(arroba);
        }
        return correo.substring(0, 2) + "***" + correo.substring(arroba);
    }
}