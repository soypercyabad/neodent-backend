package com.neodent.auth.service;

import java.time.LocalDateTime;

import com.neodent.auth.dto.request.PatientRegistrationConfirmRequest;
import com.neodent.auth.dto.request.PatientRegistrationInitRequest;
import com.neodent.auth.dto.request.PatientRegistrationRequest;
import com.neodent.auth.dto.request.RestartEmailVerificationRequest;
import com.neodent.auth.dto.request.VerifyEmailRequest;
import com.neodent.auth.dto.response.PatientRegistrationCheckResponse;
import com.neodent.auth.dto.response.PatientRegistrationInitResponse;
import com.neodent.auth.dto.response.PatientRegistrationResponse;
import com.neodent.auth.dto.response.RestartEmailVerificationResponse;
import com.neodent.auth.dto.response.VerifyEmailResponse;
import com.neodent.auth.model.CodigoVerificacion;
import com.neodent.dni.DniService;
import com.neodent.dni.dto.DniResponse;
import com.neodent.paciente.dto.CrearPacienteRequest;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.model.TipoDocumento;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.paciente.repository.TipoDocumentoRepository;
import com.neodent.paciente.service.PacienteService;
import com.neodent.legal.model.AceptacionTerminos;
import com.neodent.legal.model.TerminosCondiciones;
import com.neodent.legal.repository.AceptacionTerminosRepository;
import com.neodent.legal.repository.TerminosCondicionesRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.EstadoUsuario;
import com.neodent.usuario.model.Rol;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.EstadoUsuarioRepository;
import com.neodent.usuario.repository.RolRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientRegistrationService {
    private final PacienteRepository pacienteRepository;
    private final AceptacionTerminosRepository aceptacionTerminosRepository;
    private final TerminosCondicionesRepository terminosCondicionesRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final DniService dniService;
    private final TurnstileService turnstileService;
    private final RegistrationRateLimitService rateLimitService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final PacienteService pacienteService;
    private final OtpService otpService;

    public PatientRegistrationCheckResponse verificarDocumento(String codigo, String numeroDocumento, String turnstileToken, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(turnstileToken, ip);

        TipoDocumento tipo = buscarTipoDocumento(codigo);
        String documento = normalizarYValidarDocumento(tipo, numeroDocumento);

        if (pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(tipo.getCodigo(), documento))
            throw new ConflictException("Ya existe un paciente registrado con este documento");

        if (!"DNI".equalsIgnoreCase(tipo.getCodigo()))
            return new PatientRegistrationCheckResponse(tipo.getCodigo(), documento, null, null, null, true);

        try {
            DniResponse response = dniService.buscarPorDni(documento);
            return new PatientRegistrationCheckResponse(
                tipo.getCodigo(), documento, response.nombres(),
                response.apellidoPaterno(), response.apellidoMaterno(), false
            );
        } catch (BusinessException ex) {
            return new PatientRegistrationCheckResponse(tipo.getCodigo(), documento, null, null, null, true);
        }
    }

    /* Compatibilidad temporal con /check-dni mientras migras el frontend. */
    public PatientRegistrationCheckResponse verificarDni(String dni, String turnstileToken, String ip) {
        return verificarDocumento("DNI", dni, turnstileToken, ip);
    }

    public PatientRegistrationInitResponse iniciarRegistro(PatientRegistrationInitRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);

        TipoDocumento tipo = buscarTipoDocumento(request.tipoDocumento());
        String documento = normalizarYValidarDocumento(tipo, request.numeroDocumento());
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreoIgnoreCase(email))
            throw new ConflictException("Ya existe una cuenta registrada con este correo");

        if (pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(tipo.getCodigo(), documento))
            throw new ConflictException("Ya existe un paciente registrado con este documento");

        OtpService.OtpGenerado otp = otpService.generarRegistroEmailVerification(email);

        return new PatientRegistrationInitResponse(
            otp.id(),
            "Código de verificación enviado al correo electrónico"
        );
    }

    @Transactional
    public PatientRegistrationResponse confirmarRegistro(PatientRegistrationConfirmRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        CodigoVerificacion codigoEntity = otpService.verificarRegistroEmailOtp(request.challengeId(), request.codigo());

        if (!email.equalsIgnoreCase(codigoEntity.getCorreoDestino())) {
            throw new BusinessException("El código no corresponde a este correo electrónico");
        }

        TipoDocumento tipo = buscarTipoDocumento(request.tipoDocumento());
        String documento = normalizarYValidarDocumento(tipo, request.numeroDocumento());

        if (usuarioRepository.existsByCorreoIgnoreCase(email))
            throw new ConflictException("Ya existe una cuenta registrada con este correo");

        if (pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(tipo.getCodigo(), documento))
            throw new ConflictException("Ya existe un paciente registrado con este documento");

        Rol rolPaciente = rolRepository.findByNombreAndActivoTrue(AppConstants.Roles.PACIENTE)
            .orElseThrow(() -> new IllegalStateException("Rol PACIENTE no configurado"));
        EstadoUsuario activo = estadoUsuarioRepository.findByNombreAndActivoTrue(AppConstants.EstadosUsuario.ACTIVO)
            .orElseThrow(() -> new IllegalStateException("Estado ACTIVO no configurado"));

        Usuario usuario = new Usuario();
        usuario.setAliasInterno(null);
        usuario.setCorreo(email);
        usuario.setHashContrasena(passwordEncoder.encode(request.password()));
        usuario.getRoles().add(rolPaciente);
        usuario.setEstado(activo);
        usuario.setSegundoFactor(true);
        usuario.setCorreoVerificado(true);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        registrarAceptacion(usuarioGuardado);

        codigoEntity.setUsuario(usuarioGuardado);

        CrearPacienteRequest pacienteRequest = new CrearPacienteRequest(
            tipo.getCodigo(), documento, request.nombres(), request.apellidoPaterno(),
            request.apellidoMaterno(), request.fechaNacimiento(), request.telefono(),
            email, request.direccion()
        );

        Paciente paciente = pacienteService.crearConUsuario(pacienteRequest, usuarioGuardado);
        

        return new PatientRegistrationResponse(
            paciente.getId(),
            usuarioGuardado.getId(),
            codigoEntity.getId(),
            "Cuenta registrada y verificada con éxito"
        );
    }

    @Transactional
    public PatientRegistrationResponse registrar(PatientRegistrationRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);

        TipoDocumento tipo = buscarTipoDocumento(request.tipoDocumento());
        String documento = normalizarYValidarDocumento(tipo, request.numeroDocumento());
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByCorreoIgnoreCase(email))
            throw new ConflictException("Ya existe una cuenta registrada con este correo");

        if (pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(tipo.getCodigo(), documento))
            throw new ConflictException("Ya existe un paciente registrado con este documento");

        Rol rolPaciente = rolRepository.findByNombreAndActivoTrue(AppConstants.Roles.PACIENTE)
            .orElseThrow(() -> new IllegalStateException("Rol PACIENTE no configurado"));
        EstadoUsuario pendiente = estadoUsuarioRepository.findByNombreAndActivoTrue(AppConstants.EstadosUsuario.PENDIENTE)
            .orElseThrow(() -> new IllegalStateException("Estado PENDIENTE no configurado"));

        Usuario usuario = new Usuario();
        usuario.setAliasInterno(null);
        usuario.setCorreo(email);
        usuario.setHashContrasena(passwordEncoder.encode(request.password()));
        usuario.getRoles().add(rolPaciente);
        usuario.setEstado(pendiente);
        usuario.setSegundoFactor(true);
        usuario.setCorreoVerificado(false);
        Usuario usuarioGuardado = usuarioRepository.save(usuario);
        registrarAceptacion(usuarioGuardado);

        CrearPacienteRequest pacienteRequest = new CrearPacienteRequest(
            tipo.getCodigo(), documento, request.nombres(), request.apellidoPaterno(),
            request.apellidoMaterno(), request.fechaNacimiento(), request.telefono(),
            email, request.direccion()
        );

        Paciente paciente = pacienteService.crearConUsuario(pacienteRequest, usuarioGuardado);
        
        OtpService.OtpGenerado otp = otpService.generarEmailVerification(usuarioGuardado);

        return new PatientRegistrationResponse(
            paciente.getId(), usuarioGuardado.getId(), otp.id(),
            "Registro creado. Verifique su correo electrónico."
        );
    }

    @Transactional
    public VerifyEmailResponse verificarEmail(VerifyEmailRequest request) {
        Usuario usuario = otpService.verificarEmailOtp(request.challengeId(), request.codigo());
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado()))
            throw new ConflictException("El correo electrónico ya fue verificado");

        EstadoUsuario activo = estadoUsuarioRepository.findByNombreAndActivoTrue(AppConstants.EstadosUsuario.ACTIVO)
            .orElseThrow(() -> new IllegalStateException("Estado ACTIVO no configurado"));

        usuario.setCorreoVerificado(true);
        usuario.setEstado(activo);
        usuarioRepository.save(usuario);
        return new VerifyEmailResponse(true, "Correo verificado. La cuenta ya se encuentra activa.");
    }

    @Transactional
    public RestartEmailVerificationResponse reiniciarVerificacionEmail(RestartEmailVerificationRequest request, String ip) {
        rateLimitService.validar(ip);
        turnstileService.validar(request.turnstileToken(), ip);
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(email)
            .orElseThrow(() -> new UnauthorizedException("Correo o contraseña incorrectos"));

        if (!passwordEncoder.matches(request.password(), usuario.getHashContrasena()))
            throw new UnauthorizedException("Correo o contraseña incorrectos");
        if (Boolean.TRUE.equals(usuario.getCorreoVerificado()))
            throw new ConflictException("El correo electrónico ya fue verificado");
        if (!AppConstants.EstadosUsuario.PENDIENTE.equalsIgnoreCase(usuario.getEstado().getNombre()))
            throw new ConflictException("La cuenta no se encuentra pendiente de verificación");

        OtpService.OtpGenerado nuevo = otpService.reiniciarEmailVerification(usuario);
        return new RestartEmailVerificationResponse(nuevo.id(), "Se envió un nuevo código de verificación");
    }

    private TipoDocumento buscarTipoDocumento(String codigo) {
        if (codigo == null || codigo.isBlank()) throw new BusinessException("Selecciona un tipo de documento");
        return tipoDocumentoRepository.findByCodigoAndActivoTrue(codigo.trim().toUpperCase(Locale.ROOT))
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no válido"));
    }

    private String normalizarYValidarDocumento(TipoDocumento tipo, String valor) {
        if (valor == null || valor.isBlank()) throw new BusinessException("El número de documento es obligatorio");

        String documento = valor.trim();
        if (!"DNI".equalsIgnoreCase(tipo.getCodigo())) documento = documento.toUpperCase(Locale.ROOT);

        int longitud = documento.length();
        int minimo = tipo.getLongitudMin() == null ? 1 : tipo.getLongitudMin();
        int maximo = tipo.getLongitudMax() == null ? 20 : tipo.getLongitudMax();

        if (longitud < minimo || longitud > maximo)
            throw new BusinessException("El documento debe tener entre " + minimo + " y " + maximo + " caracteres");

        if ("DNI".equalsIgnoreCase(tipo.getCodigo()) && !documento.matches("\\d{8}"))
            throw new BusinessException("El DNI debe contener exactamente 8 dígitos");

        return documento;
    }

    private void registrarAceptacion(Usuario usuario) {
        try {
            terminosCondicionesRepository.findFirstByVigenteTrueOrderByFechaPublicacionDesc().ifPresent(terminos -> {
                if (!aceptacionTerminosRepository.existsByUsuarioIdAndTerminosId(usuario.getId(), terminos.getId())) {
                    AceptacionTerminos aceptacion = new AceptacionTerminos();
                    aceptacion.setUsuario(usuario);
                    aceptacion.setTerminos(terminos);
                    aceptacion.setAceptadoEn(LocalDateTime.now());
                    aceptacionTerminosRepository.save(aceptacion);
                }
            });
        } catch (Exception ex) {
            log.warn("No se pudo registrar aceptación de términos para usuario {}: {}", usuario.getId(), ex.getMessage());
        }
    }
}
