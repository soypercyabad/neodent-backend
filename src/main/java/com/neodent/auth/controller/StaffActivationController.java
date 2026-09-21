package com.neodent.auth.controller;

import com.neodent.auth.dto.request.*;
import com.neodent.auth.dto.response.*;
import com.neodent.auth.service.StaffActivationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/staff-activation")
@RequiredArgsConstructor
public class StaffActivationController {

    private final StaffActivationService service;

    @PostMapping("/validate")
    public AccountInvitationResponse validar(
        @Valid @RequestBody ValidateAccountInvitationRequest request
    ) {
        return service.validar(request.token());
    }

    @PostMapping("/start")
    public StartAccountActivationResponse iniciar(
        @Valid @RequestBody StartStaffActivationRequest request,
        HttpServletRequest httpRequest
    ) {
        return service.iniciar(request, httpRequest.getRemoteAddr());
    }

    @PostMapping("/complete")
    public CompleteAccountActivationResponse completar(
        @Valid @RequestBody CompleteStaffActivationRequest request
    ) {
        return service.completar(request);
    }
}