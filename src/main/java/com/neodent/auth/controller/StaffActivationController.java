package com.neodent.auth.controller;

import com.neodent.auth.dto.request.*;
import com.neodent.auth.dto.response.*;
import com.neodent.auth.service.StaffActivationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/staff-activation")
@RequiredArgsConstructor
@Tag(name = "Activación de personal", description = "Operaciones de activación de personal")
public class StaffActivationController {

    private final StaffActivationService service;



    @Operation(
        summary = "Validar invitación de personal",
        description = "Valida el token de invitación para activación de cuenta de personal"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitación válida"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Invitación inválida o expirada"),
        @ApiResponse(responseCode = "409", description = "El personal ya cuenta con una cuenta activa")
    })
    @PostMapping("/validate")
    public AccountInvitationResponse validar(
        @Valid @RequestBody ValidateAccountInvitationRequest request
    ) {
        return service.validar(request.token());
    }



    @Operation(
        summary = "Iniciar activación de personal",
        description = "Inicia el proceso de activación enviando un código OTP de verificación al correo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Proceso de activación iniciado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Token de invitación inválido o expirado"),
        @ApiResponse(responseCode = "429", description = "Demasiadas solicitudes")
    })
    @PostMapping("/start")
    public StartAccountActivationResponse iniciar(
        @Valid @RequestBody StartStaffActivationRequest request,
        HttpServletRequest httpRequest
    ) {
        return service.iniciar(request, httpRequest.getRemoteAddr());
    }



    @Operation(
        summary = "Completar activación de personal",
        description = "Valida el código OTP y establece la contraseña definitiva de la cuenta de personal"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activación de cuenta completada"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o contraseñas no coinciden"),
        @ApiResponse(responseCode = "401", description = "Token u OTP inválido o expirado")
    })
    @PostMapping("/complete")
    public CompleteAccountActivationResponse completar(
        @Valid @RequestBody CompleteStaffActivationRequest request
    ) {
        return service.completar(request);
    }
}