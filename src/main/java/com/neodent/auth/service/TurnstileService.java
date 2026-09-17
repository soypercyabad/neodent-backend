package com.neodent.auth.service;

import com.neodent.auth.dto.response.TurnstileResponse;
import com.neodent.shared.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TurnstileService {

    private final RestClient restClient;

    @Value("${turnstile.secret}")
    private String secret;

    @Value("${turnstile.verify-url}")
    private String verifyUrl;

    public void validar(String token, String ip) {
        TurnstileResponse response = restClient
            .post()
            .uri(verifyUrl)
            .body(Map.of(
                "secret", secret,
                "response", token,
                "remoteip", ip
            ))
            .retrieve()
            .body(TurnstileResponse.class);

        if (response == null || !response.success()) {
            throw new ForbiddenException("No se pudo validar la verificación de seguridad");
        }
    }
}