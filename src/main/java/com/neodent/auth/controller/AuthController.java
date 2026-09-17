package com.neodent.auth.controller;

import com.neodent.auth.dto.request.CompleteAccountActivationRequest;
import com.neodent.auth.dto.request.LoginRequest;
import com.neodent.auth.dto.request.PatientRegistrationCheckRequest;
import com.neodent.auth.dto.request.PatientRegistrationRequest;
import com.neodent.auth.dto.request.ResendCodeRequest;
import com.neodent.auth.dto.request.RestartEmailVerificationRequest;
import com.neodent.auth.dto.request.StartAccountActivationRequest;
import com.neodent.auth.dto.request.ValidateAccountInvitationRequest;
import com.neodent.auth.dto.request.VerifyEmailRequest;
import com.neodent.auth.dto.request.VerifyTwoFactorRequest;
import com.neodent.auth.dto.response.AccountInvitationResponse;
import com.neodent.auth.dto.response.CompleteAccountActivationResponse;
import com.neodent.auth.dto.response.LoginResponse;
import com.neodent.auth.dto.response.PatientRegistrationCheckResponse;
import com.neodent.auth.dto.response.PatientRegistrationResponse;
import com.neodent.auth.dto.response.ResendCodeResponse;
import com.neodent.auth.dto.response.RestartEmailVerificationResponse;
import com.neodent.auth.dto.response.StartAccountActivationResponse;
import com.neodent.auth.dto.response.VerifyEmailResponse;
import com.neodent.auth.dto.response.VerifyTwoFactorResponse;
import com.neodent.auth.service.AccountActivationService;
import com.neodent.auth.service.AuthService;
import com.neodent.auth.service.OtpService;
import com.neodent.auth.service.PatientRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Operaciones de autenticación y seguridad de NeoDent")
public class AuthController {

    private final AuthService authService;
    private final PatientRegistrationService patientRegistrationService;
    private final OtpService otpService;
    private final AccountActivationService accountActivationService;


    @Operation(
        summary = "Iniciar sesión",
        description = """
            Valida el correo electrónico y la contraseña.
            Si las credenciales son correctas y el usuario tiene
            segundo factor habilitado, genera un challenge y envía
            un código OTP al correo registrado.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Credenciales correctas"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos"),
        @ApiResponse(responseCode = "403", description = "Cuenta pendiente, bloqueada o inactiva")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }



    @Operation(
        summary = "Verificar segundo factor",
        description = """
            Valida el código OTP enviado durante el inicio de sesión.
            Si es correcto, completa la autenticación
            y genera el Access Token JWT.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Código verificado correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Código inválido, expirado o ya utilizado")
    })
    @PostMapping("/verify-2fa")
    public VerifyTwoFactorResponse verificarTwoFactor(@Valid @RequestBody VerifyTwoFactorRequest request) {
        return authService.verificarTwoFactor(request);
    }



    @Operation(
        summary = "Validar DNI para autoregistro",
        description = """
            Valida si un DNI puede iniciar el autoregistro.
            Primero consulta NeoDent.
            Si el paciente ya existe, bloquea el registro.
            Si no existe, intenta obtener sus datos desde el proveedor externo de DNI.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "DNI disponible para registro"),
        @ApiResponse(responseCode = "400", description = "Formato de DNI inválido"),
        @ApiResponse(responseCode = "409", description = "El paciente ya está registrado"),
        @ApiResponse(responseCode = "429", description = "Demasiadas consultas"),
        @ApiResponse(responseCode = "403", description = "Verificación Turnstile inválida")
    })
    @PostMapping("/patient-registration/check-dni")
    public PatientRegistrationCheckResponse verificarDniRegistro(
        @Valid @RequestBody PatientRegistrationCheckRequest request,
        HttpServletRequest httpRequest
    ) {
        String ip = obtenerIpCliente(httpRequest);
        return patientRegistrationService.verificarDni(request.dni(), request.turnstileToken(), ip);
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr();
    }



    @Operation(
        summary = "Registrar paciente",
        description = """
            Registra un nuevo paciente y crea su cuenta de acceso en estado pendiente.
            Envía un código para verificar el correo.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro creado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "403", description = "Verificación Turnstile inválida"),
        @ApiResponse(responseCode = "409", description = "Paciente, correo o teléfono ya registrado"),
        @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    @PostMapping("/patient-registration")
    public PatientRegistrationResponse registrarPaciente(
        @Valid @RequestBody PatientRegistrationRequest request,
        HttpServletRequest httpRequest
    ) {
        String ip = obtenerIpCliente(httpRequest);
        return patientRegistrationService.registrar(request, ip);
    }



    @Operation(
        summary = "Verificar correo del paciente",
        description = """
            Valida el código enviado durante el autoregistro.
            Si es correcto, verifica el correo y activa la cuenta.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Correo verificado y cuenta activada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Código incorrecto, expirado o utilizado"),
        @ApiResponse(responseCode = "409", description = "Correo ya verificado")
    })
    @PostMapping("/patient-registration/verify-email")
    public VerifyEmailResponse verificarEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return patientRegistrationService.verificarEmail(request);
    }



    @Operation(
        summary = "Reenviar código de verificación",
        description = "Invalida el código anterior y genera un nuevo código para el mismo proceso de verificación."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nuevo código enviado"),
        @ApiResponse(responseCode = "401", description = "Challenge inválido")
    })
    @PostMapping("/resend-code")
    public ResendCodeResponse reenviarCodigo(@Valid @RequestBody ResendCodeRequest request) {
        OtpService.OtpGenerado nuevo = otpService.reenviarCodigo(request.challengeId());
        return new ResendCodeResponse(nuevo.id(), "Se envió un nuevo código de verificación");
    }



    @Operation(
        summary = "Reiniciar verificación de correo",
        description = """
            Inicia un nuevo ciclo de verificación para una cuenta pendiente que agotó o perdió sus códigos anteriores.
            Requiere correo, contraseña y verificación Turnstile.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Nuevo código enviado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos"),
        @ApiResponse(responseCode = "403", description = "Verificación Turnstile inválida"),
        @ApiResponse(responseCode = "409", description = "La cuenta ya está verificada o no está pendiente"),
        @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    @PostMapping("/patient-registration/restart-verification")
    public RestartEmailVerificationResponse reiniciarVerificacionEmail(
        @Valid @RequestBody RestartEmailVerificationRequest request,
        HttpServletRequest httpRequest
    ) {
        String ip = obtenerIpCliente(httpRequest);
        return patientRegistrationService.reiniciarVerificacionEmail(request, ip);
    }



    @Operation(
        summary = "Validar invitación de cuenta",
        description = "Valida el enlace de activación enviado a un paciente registrado por recepción."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitación válida"),
        @ApiResponse(responseCode = "401", description = "Invitación inválida o expirada"),
        @ApiResponse(responseCode = "409", description = "El paciente ya tiene cuenta")
    })
    @PostMapping("/account-activation/validate")
    public AccountInvitationResponse validarInvitacion(@Valid @RequestBody ValidateAccountInvitationRequest request) {
        return accountActivationService.validar(request.token());
    }



    @Operation(
        summary = "Iniciar activación de cuenta",
        description = """
            Valida el token de invitación y el captcha Turnstile.
            Si son válidos, genera y envía un código OTP al correo del paciente para continuar con la activación de su cuenta.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Código de activación enviado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos (token o captcha faltante)"),
        @ApiResponse(responseCode = "401", description = "Token de invitación inválido o expirado"),
        @ApiResponse(responseCode = "403", description = "Verificación Turnstile inválida"),
        @ApiResponse(responseCode = "409", description = "El paciente ya cuenta con un usuario activo"),
        @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    @PostMapping("/account-activation/start")
    public StartAccountActivationResponse iniciarActivacion(
        @Valid @RequestBody StartAccountActivationRequest request,
        HttpServletRequest httpRequest
    ) {
        String ip = obtenerIpCliente(httpRequest);
        return accountActivationService.iniciar(request, ip);
    }



    @Operation(
        summary = "Completar activación de cuenta",
        description = "Valida la invitación y el código OTP, crea la cuenta del paciente y la vincula con su registro existente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cuenta activada"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "401", description = "Token u OTP inválido"),
        @ApiResponse(responseCode = "409", description = "El paciente ya tiene cuenta")
    })
    @PostMapping("/account-activation/complete")
    public CompleteAccountActivationResponse completarActivacion(@Valid @RequestBody CompleteAccountActivationRequest request) {
        return accountActivationService.completar(request);
    }
}