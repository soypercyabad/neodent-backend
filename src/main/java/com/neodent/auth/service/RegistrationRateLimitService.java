package com.neodent.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.neodent.shared.exception.TooManyRequestsException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class RegistrationRateLimitService {

    private static final int MAX_REQUESTS = 5;
    private static final long WINDOW_SECONDS = 60;

    private final Cache<String, RateData> requests = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(5))
        .maximumSize(10_000)
        .build();

    public void validar(String ip) {
        long ahora = Instant.now().getEpochSecond();

        requests.asMap().compute(ip, (key, data) -> {
            if (data == null || ahora - data.inicio() >= WINDOW_SECONDS) {
                return new RateData(ahora, 1);
            }

            if (data.intentos() >= MAX_REQUESTS) {
                throw new TooManyRequestsException("Demasiadas consultas. Intente nuevamente en un minuto");
            }

            return new RateData(data.inicio(), data.intentos() + 1);
        });
    }

    private record RateData(long inicio, int intentos) {}
}