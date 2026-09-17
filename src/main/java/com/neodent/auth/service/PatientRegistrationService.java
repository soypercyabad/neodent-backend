package com.neodent.auth.service;

import com.neodent.auth.dto.request.PatientRegistrationRequest;
import com.neodent.auth.dto.request.RestartEmailVerificationRequest;
import com.neodent.auth.dto.request.VerifyEmailRequest;
import com.neodent.auth.dto.response.PatientRegistrationCheckResponse;
import com.neodent.auth.dto.response.PatientRegistrationResponse;
import com.neodent.auth.dto.response.RestartEmailVerificationResponse;
import com.neodent.auth.dto.response.VerifyEmailResponse;
import com.neodent.dni.DniService;
import com.neodent.dni.dto.DniResponse;
import com.neodent.paciente.dto.CrearPacienteRequest;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.paciente.service.PacienteService;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.BusinessException;
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
public class PatientRegistrationService {

    private final PacienteRepository pacienteRepository;
    private final DniService dniService;
    private final TurnstileService turnstileService;
    private final RegistrationRateLimitService rateLimitService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PacienteService pacienteService;
    private final OtpService otpService;

    public PatientRegistrationCheckResponse verificarDni(String dni, String turnstileToken, String ip) {
        String numeroDocumento = dni.trim();
        rateLimitService.validar(ip);
        turnstileService.validar(turnstileToken, ip);

        boolean existe = pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(
            AppConstants.TiposDocumento.DNI,
            numeroDocumento
        );

        if (existe) {
            throw new ConflictException("Ya existe un paciente registrado con este DNI");
        }

        try {
            DniResponse response = dniService.buscarPorDni(numeroDocumento);
            return new PatientRegistrationCheckResponse(
                response.dni(),
                response.nombres(),
                response.apellidoPaterno(),
                response.apellidoMaterno(),
                false
            );
        } catch (BusinessException ex) {
            return new PatientRegistrationCheckResponse(numeroDocumento, null, null, null, true);
        }
    }

    @Transactional
    public PatientRegistrationResponse registrar(PatientRegistrationRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);

        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta registrada con este correo");
        }

        Rol rolPaciente = rolRepository.findByNombreAndActivoTrue(AppConstants.Roles.PACIENTE)
            .orElseThrow(() -> new IllegalStateException("Rol PACIENTE no configurado"));

        EstadoUsuario pendiente = estadoUsuarioRepository.findByNombreAndActivoTrue(AppConstants.EstadosUsuario.PENDIENTE)
            .orElseThrow(() -> new IllegalStateException("Estado PENDIENTE no configurado"));

        Usuario usuario = new Usuario();
        usuario.setUsername(null);
        usuario.setEmail(email);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(rolPaciente);
        usuario.setEstado(pendiente);
        usuario.setTwoFactorEnabled(true);
        usuario.setEmailVerificado(false);

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        CrearPacienteRequest pacienteRequest = new CrearPacienteRequest(
            AppConstants.TiposDocumento.DNI,
            request.dni(),
            request.nombres(),
            request.apellidoPaterno(),
            request.apellidoMaterno(),
            request.fechaNacimiento(),
            request.telefono(),
            email,
            request.direccion()
        );

        Paciente paciente = pacienteService.crearConUsuario(pacienteRequest, usuarioGuardado);
        OtpService.OtpGenerado otp = otpService.generarEmailVerification(usuarioGuardado);

        return new PatientRegistrationResponse(
            paciente.getId(),
            usuarioGuardado.getId(),
            otp.id(),
            "Registro creado. Verifique su correo electrónico."
        );
    }

    @Transactional
    public VerifyEmailResponse verificarEmail(VerifyEmailRequest request) {
        Usuario usuario = otpService.verificarEmailOtp(request.challengeId(), request.codigo());

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new ConflictException("El correo electrónico ya fue verificado");
        }

        EstadoUsuario activo = estadoUsuarioRepository.findByNombreAndActivoTrue(AppConstants.EstadosUsuario.ACTIVO)
            .orElseThrow(() -> new IllegalStateException("Estado ACTIVO no configurado"));

        usuario.setEmailVerificado(true);
        usuario.setEstado(activo);
        usuarioRepository.save(usuario);

        return new VerifyEmailResponse(true, "Correo verificado. La cuenta ya se encuentra activa.");
    }

    @Transactional
    public RestartEmailVerificationResponse reiniciarVerificacionEmail(RestartEmailVerificationRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);

        String email = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new UnauthorizedException("Correo o contraseña incorrectos");
        }

        if (Boolean.TRUE.equals(usuario.getEmailVerificado())) {
            throw new ConflictException("El correo electrónico ya fue verificado");
        }

        if (!AppConstants.EstadosUsuario.PENDIENTE.equalsIgnoreCase(usuario.getEstado().getNombre())) {
            throw new ConflictException("La cuenta no se encuentra pendiente de verificación");
        }

        OtpService.OtpGenerado nuevo = otpService.reiniciarEmailVerification(usuario);
        return new RestartEmailVerificationResponse(nuevo.id(), "Se envió un nuevo código de verificación");
    }
}