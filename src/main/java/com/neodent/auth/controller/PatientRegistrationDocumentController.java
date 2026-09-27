package com.neodent.auth.controller;

import com.neodent.auth.dto.request.PatientDocumentCheckRequest;
import com.neodent.auth.dto.response.PatientRegistrationCheckResponse;
import com.neodent.auth.service.PatientRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/auth/patient-registration")
@RequiredArgsConstructor
@Tag(name = "Registro de Pacientes", description = "Endpoints públicos para registro de pacientes")
public class PatientRegistrationDocumentController {
    private final PatientRegistrationService patientRegistrationService;

    @PostMapping("/check-documento")
    public PatientRegistrationCheckResponse verificar(@Valid @RequestBody PatientDocumentCheckRequest request, HttpServletRequest httpRequest) {
        return patientRegistrationService.verificarDocumento(
            request.tipoDocumento(),
            request.numeroDocumento(),
            request.turnstileToken(),
            obtenerIpCliente(httpRequest)
        );
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String cfIp = request.getHeader("CF-Connecting-IP");
        return cfIp != null && !cfIp.isBlank() ? cfIp : request.getRemoteAddr();
    }
}