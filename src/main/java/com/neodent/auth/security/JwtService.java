package com.neodent.auth.security;

import com.neodent.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generarAccessToken(Usuario usuario) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("neodent-api")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expirationMinutes * 60))
            .subject(usuario.getId().toString())
            .claim("role", usuario.getRol().getNombre())
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}