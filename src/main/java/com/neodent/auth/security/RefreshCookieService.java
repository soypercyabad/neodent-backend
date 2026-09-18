package com.neodent.auth.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Arrays;

@Service
public class RefreshCookieService {

    @Value("${auth.refresh.cookie-name}")
    private String cookieName;

    @Value("${auth.refresh.cookie-secure}")
    private boolean secure;

    @Value("${auth.refresh.cookie-same-site}")
    private String sameSite;

    public void guardar(HttpServletResponse response, String token, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/auth")
            .maxAge(Duration.ofSeconds(maxAgeSeconds))
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String obtener(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    public void eliminar(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .sameSite(sameSite)
            .path("/api/auth")
            .maxAge(Duration.ZERO)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}