package com.neodent.auth.service;

import com.neodent.auth.dto.request.LoginRequest;
import com.neodent.auth.dto.request.VerifyTwoFactorRequest;
import com.neodent.auth.dto.response.LoginResponse;
import com.neodent.auth.dto.response.VerifyTwoFactorResponse;
import com.neodent.auth.security.JwtService;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ForbiddenException;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final JwtService jwtService;


    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email = request.email()
            .trim()
            .toLowerCase();

        Usuario usuario = usuarioRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() ->
                new UnauthorizedException(
                    "Correo o contraseña incorrectos"
                )
            );


        boolean passwordValido =
            passwordEncoder.matches(
                request.password(),
                usuario.getPasswordHash()
            );


        if (!passwordValido) {
            throw new UnauthorizedException(
                "Correo o contraseña incorrectos"
            );
        }


        String estado =
            usuario.getEstado()
                .getNombre();


        if (AppConstants.EstadosUsuario.BLOQUEADO.equalsIgnoreCase(estado)) {
            throw new ForbiddenException(
                "La cuenta se encuentra bloqueada"
            );
        }


        if (AppConstants.EstadosUsuario.INACTIVO.equalsIgnoreCase(estado)) {
            throw new ForbiddenException(
                "La cuenta se encuentra inactiva"
            );
        }


        if (AppConstants.EstadosUsuario.PENDIENTE.equalsIgnoreCase(estado)) {
            throw new ForbiddenException(
                "La cuenta aún no ha sido activada"
            );
        }


        if (usuario.getTwoFactorEnabled()) {

            OtpService.OtpGenerado otp =
                otpService.generarLoginOtp(usuario);

            return new LoginResponse(
                true,
                otp.id(),
                "Se requiere verificación en dos pasos"
            );
        }

        return new LoginResponse(
            false,
            null,
            "Credenciales correctas"
        );
    }


    public VerifyTwoFactorResponse verificarTwoFactor(
        VerifyTwoFactorRequest request
    ) {

        Usuario usuario =
            otpService.verificarLoginOtp(
                request.challengeId(),
                request.codigo()
            );

        String accessToken =
            jwtService.generarAccessToken(usuario);

        return new VerifyTwoFactorResponse(
            true,
            accessToken,
            "Bearer",
            900,
            "Autenticación completada correctamente"
        );
    }
}