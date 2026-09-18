package com.neodent.config;

import com.neodent.shared.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                /* Públicos */
                .requestMatchers(
                    "/api/health/**",
                    "/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()

                /* Autenticación / registro / activación */
                .requestMatchers(
                    "/api/auth/**"
                ).permitAll()

                /* Gestión administrativa de pacientes */
                .requestMatchers(
                    "/api/pacientes/**"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA
                )

                /* DNI administrativo */
                .requestMatchers(
                    "/api/dni/**"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA
                )

                /* Citas*/
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/citas/disponibilidad"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA,
                    AppConstants.Roles.PACIENTE
                )

                /* Crear HOLD y convertir HOLD en una cita PROGRAMADA.*/
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/citas/hold",
                    "/api/citas/confirm"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA,
                    AppConstants.Roles.PACIENTE
                )

                /* El paciente podrá consultar únicamente sus propias citas.*/
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/citas/mis-citas"
                ).hasRole(
                    AppConstants.Roles.PACIENTE
                )

                /* Agenda propia del odontólogo. */
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/citas/mi-agenda"
                ).hasRole(
                    AppConstants.Roles.ODONTOLOGO
                )

                /* Flujo clínico. Además de SecurityConfig, CitaService ya valida que la cita pertenezca al odontólogo. */
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/citas/*/iniciar-atencion",
                    "/api/citas/*/finalizar-atencion"
                ).hasRole(
                    AppConstants.Roles.ODONTOLOGO
                )

                /* Gestión administrativa de cita. PACIENTE NO puede hacer esto. */
                .requestMatchers(
                    HttpMethod.PUT,
                    "/api/citas/*/reprogramar",
                    "/api/citas/*/cancelar",
                    "/api/citas/*/confirmar-asistencia",
                    "/api/citas/*/no-asistio"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA
                )

                /* Cualquier otra operación de citas queda para Administración/Recepción.*/
                .requestMatchers(
                    "/api/citas/**"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA
                )

                /* Horarios y bloqueos */
                .requestMatchers(
                    "/api/horarios/**",
                    "/api/bloqueos/**"
                ).hasAnyRole(
                    AppConstants.Roles.ADMIN,
                    AppConstants.Roles.RECEPCIONISTA
                )

                /* Usuarios internos */
                .requestMatchers(
                    "/api/usuarios-internos/**"
                ).hasRole(
                    AppConstants.Roles.ADMIN
                )

                /* Todo lo no definido explícitamente queda bloqueado.*/
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            List<String> roles = jwt.getClaimAsStringList("roles");
            if (roles == null) {
                return List.of();
            }
            Collection<GrantedAuthority> authorities = roles.stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(AppConstants.Roles.PREFIX + role))
                .toList();
            return authorities;
        });
        return converter;
    }
}