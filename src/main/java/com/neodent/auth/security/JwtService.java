package com.neodent.auth.security;

import com.neodent.usuario.model.Rol;
import com.neodent.usuario.model.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;

    @Value("${jwt.expiration-minutes}")
    private long expirationMinutes;

    public String generarAccessToken(Usuario usuario) {

        Instant now = Instant.now();

        List<String> roles = usuario.getRoles().stream()
            .filter(rol -> Boolean.TRUE.equals(rol.getActivo()))
            .map(Rol::getNombre)
            .sorted()
            .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .issuer("neodent-api")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(expirationMinutes * 60))
            .subject(usuario.getId().toString())
            .claim("roles", roles)
            .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationMinutes * 60;
    }
}