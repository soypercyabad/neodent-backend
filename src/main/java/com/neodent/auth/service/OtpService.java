package com.neodent.auth.service;

import com.neodent.auth.model.CodigoVerificacion;
import com.neodent.auth.repository.CodigoVerificacionRepository;
import com.neodent.notification.EmailService;
import com.neodent.paciente.model.Paciente;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OtpService {

    private static final String TIPO_LOGIN = AppConstants.TiposOtp.LOGIN_2FA;
    private static final String TIPO_EMAIL = AppConstants.TiposOtp.EMAIL_VERIFICATION;
    private static final String TIPO_ACTIVACION = AppConstants.TiposOtp.ACCOUNT_ACTIVATION;
    private static final int EXPIRACION_MINUTOS = 5;
    private static final int MAX_REENVIOS = 3;
    private static final int COOLDOWN_REENVIO_SEGUNDOS = 60;

    private final CodigoVerificacionRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public record OtpGenerado(Long id, String codigo) {}

    @Transactional
    public OtpGenerado generarLoginOtp(Usuario usuario) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion otp = new CodigoVerificacion();
        otp.setUsuario(usuario);
        otp.setEmailDestino(usuario.getEmail());
        otp.setTipo(TIPO_LOGIN);
        otp.setCodigoHash(passwordEncoder.encode(codigo));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS));
        otp.setIntentos((short) 0);
        otp.setMaxIntentos((short) 5);
        otp.setUsado(false);

        CodigoVerificacion guardado = repository.save(otp);
        emailService.enviarOtpLogin(usuario.getEmail(), codigo);

        return new OtpGenerado(guardado.getId(), codigo);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Usuario verificarLoginOtp(Long challengeId, String codigo) {
        CodigoVerificacion otp = repository.findByIdAndTipo(challengeId, TIPO_LOGIN)
            .orElseThrow(() -> new UnauthorizedException("Código de verificación inválido"));

        if (otp.getIntentos() >= otp.getMaxIntentos()) {
            throw new UnauthorizedException("Se superó el número máximo de intentos");
        }
        if (otp.getUsado()) {
            throw new UnauthorizedException("El código ya fue utilizado");
        }
        if (LocalDateTime.now(clock).isAfter(otp.getExpiresAt())) {
            throw new UnauthorizedException("El código ha expirado");
        }

        boolean valido = passwordEncoder.matches(codigo, otp.getCodigoHash());
        if (!valido) {
            short nuevosIntentos = (short) (otp.getIntentos() + 1);
            otp.setIntentos(nuevosIntentos);

            if (nuevosIntentos >= otp.getMaxIntentos()) {
                otp.setUsado(true);
                repository.saveAndFlush(otp);
                throw new UnauthorizedException("Se superó el número máximo de intentos");
            }

            repository.saveAndFlush(otp);
            throw new UnauthorizedException("Código de verificación incorrecto");
        }

        otp.setUsado(true);
        repository.saveAndFlush(otp);

        return otp.getUsuario();
    }

    @Transactional
    public OtpGenerado generarEmailVerification(Usuario usuario) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion otp = new CodigoVerificacion();
        otp.setUsuario(usuario);
        otp.setEmailDestino(usuario.getEmail());
        otp.setTipo(TIPO_EMAIL);
        otp.setCodigoHash(passwordEncoder.encode(codigo));
        otp.setExpiresAt(LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS));
        otp.setIntentos((short) 0);
        otp.setMaxIntentos((short) 5);
        otp.setUsado(false);

        CodigoVerificacion guardado = repository.save(otp);
        emailService.enviarVerificacionEmail(usuario.getEmail(), codigo);

        return new OtpGenerado(guardado.getId(), codigo);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Usuario verificarEmailOtp(Long challengeId, String codigo) {
        CodigoVerificacion otp = repository.findByIdAndTipo(challengeId, TIPO_EMAIL)
            .orElseThrow(() -> new UnauthorizedException("Código de verificación inválido"));

        if (otp.getIntentos() >= otp.getMaxIntentos()) {
            throw new UnauthorizedException("Se superó el número máximo de intentos");
        }
        if (otp.getUsado()) {
            throw new UnauthorizedException("El código ya fue utilizado");
        }
        if (LocalDateTime.now(clock).isAfter(otp.getExpiresAt())) {
            throw new UnauthorizedException("El código ha expirado");
        }

        boolean valido = passwordEncoder.matches(codigo, otp.getCodigoHash());
        if (!valido) {
            short intentos = (short) (otp.getIntentos() + 1);
            otp.setIntentos(intentos);

            if (intentos >= otp.getMaxIntentos()) {
                otp.setUsado(true);
            }
            repository.saveAndFlush(otp);

            throw new UnauthorizedException(
                intentos >= otp.getMaxIntentos()
                    ? "Se superó el número máximo de intentos"
                    : "Código de verificación incorrecto"
            );
        }

        otp.setUsado(true);
        repository.saveAndFlush(otp);

        return otp.getUsuario();
    }

    @Transactional
    public OtpGenerado reenviarCodigo(Long challengeId) {
        CodigoVerificacion anterior = repository.findById(challengeId)
            .orElseThrow(() -> new UnauthorizedException("Código de verificación no encontrado"));

        if (anterior.getUsado()) {
            throw new UnauthorizedException("El código ya fue utilizado");
        }

        short reenvios = anterior.getResendCount() == null ? 0 : anterior.getResendCount();
        if (reenvios >= MAX_REENVIOS) {
            throw new UnauthorizedException("Se alcanzó el máximo de reenvíos permitidos");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        if (anterior.getLastResendAt() != null && ahora.isBefore(anterior.getLastResendAt().plusSeconds(COOLDOWN_REENVIO_SEGUNDOS))) {
            throw new UnauthorizedException("Espere un minuto antes de solicitar otro código");
        }

        anterior.setUsado(true);
        anterior.setResendCount((short) (reenvios + 1));
        anterior.setLastResendAt(ahora);
        repository.saveAndFlush(anterior);

        OtpGenerado nuevo = switch (anterior.getTipo()) {
            case TIPO_LOGIN -> generarLoginOtp(anterior.getUsuario());
            case TIPO_EMAIL -> generarEmailVerification(anterior.getUsuario());
            case TIPO_ACTIVACION -> generarAccountActivationOtp(anterior.getPaciente());
            default -> throw new UnauthorizedException("Tipo de verificación no permitido");
        };

        CodigoVerificacion nuevoOtp = repository.findById(nuevo.id())
            .orElseThrow(() -> new IllegalStateException("No se pudo recuperar el nuevo código"));

        nuevoOtp.setResendCount((short) (reenvios + 1));
        nuevoOtp.setLastResendAt(ahora);
        repository.saveAndFlush(nuevoOtp);

        return nuevo;
    }

    @Transactional
    public OtpGenerado reiniciarEmailVerification(Usuario usuario) {
        List<CodigoVerificacion> anteriores = repository.findAllByUsuarioIdAndTipoAndUsadoFalse(usuario.getId(), TIPO_EMAIL);
        anteriores.forEach(otp -> otp.setUsado(true));
        repository.saveAll(anteriores);

        return generarEmailVerification(usuario);
    }

    @Transactional
    public OtpGenerado generarAccountActivationOtp(Paciente paciente) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        CodigoVerificacion otp = new CodigoVerificacion();
        otp.setUsuario(null);
        otp.setPaciente(paciente);
        otp.setEmailDestino(paciente.getEmail());
        otp.setTipo(AppConstants.TiposOtp.ACCOUNT_ACTIVATION);
        otp.setCodigoHash(passwordEncoder.encode(codigo));
        otp.setExpiresAt(LocalDateTime.now(clock).plusMinutes(EXPIRACION_MINUTOS));
        otp.setIntentos((short) 0);
        otp.setMaxIntentos((short) 5);
        otp.setUsado(false);

        CodigoVerificacion guardado = repository.save(otp);
        emailService.enviarOtpActivacionCuenta(paciente.getEmail(), codigo);

        return new OtpGenerado(guardado.getId(), codigo);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Paciente verificarAccountActivationOtp(Long challengeId, String codigo) {
        CodigoVerificacion otp = repository.findByIdAndTipoAndPacienteIsNotNull(challengeId, AppConstants.TiposOtp.ACCOUNT_ACTIVATION)
            .orElseThrow(() -> new UnauthorizedException("Código de activación inválido"));

        if (otp.getIntentos() >= otp.getMaxIntentos()) {
            throw new UnauthorizedException("Se superó el número máximo de intentos");
        }
        if (otp.getUsado()) {
            throw new UnauthorizedException("El código ya fue utilizado");
        }
        if (LocalDateTime.now(clock).isAfter(otp.getExpiresAt())) {
            throw new UnauthorizedException("El código ha expirado");
        }

        if (!passwordEncoder.matches(codigo, otp.getCodigoHash())) {
            short intentos = (short) (otp.getIntentos() + 1);
            otp.setIntentos(intentos);

            if (intentos >= otp.getMaxIntentos()) {
                otp.setUsado(true);
            }
            repository.saveAndFlush(otp);

            throw new UnauthorizedException(
                intentos >= otp.getMaxIntentos()
                    ? "Se superó el número máximo de intentos"
                    : "Código de verificación incorrecto"
            );
        }

        otp.setUsado(true);
        repository.saveAndFlush(otp);

        return otp.getPaciente();
    }
}