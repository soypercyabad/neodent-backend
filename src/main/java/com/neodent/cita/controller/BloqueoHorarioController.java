package com.neodent.cita.controller;

import com.neodent.cita.dto.request.CrearBloqueoHorarioRequest;
import com.neodent.cita.dto.response.BloqueoHorarioResponse;
import com.neodent.cita.service.BloqueoHorarioService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bloqueos")
@RequiredArgsConstructor
@Tag(name = "Bloqueos", description = "Gestión de bloqueos de agenda odontológica")
public class BloqueoHorarioController {

    private final BloqueoHorarioService bloqueoService;

    @Operation(
        summary = "Listar bloqueos de agenda",
        description = "Retorna todos los bloqueos de agenda activa"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bloqueos obtenidos correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping
    public List<BloqueoHorarioResponse> listar() {
        return bloqueoService.listar();
    }



    @Operation(
        summary = "Crear bloqueo de agenda",
        description = "Crea un bloqueo de agenda odontológica"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bloqueo creado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Odontólogo o sede no encontrado")
    })
    @PostMapping
    public BloqueoHorarioResponse crear(
        @Valid @RequestBody CrearBloqueoHorarioRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return bloqueoService.crear(request, usuarioId);
    }



    @Operation(
        summary = "Eliminar bloqueo de agenda",
        description = "Elimina un bloqueo de agenda odontológica"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Bloqueo eliminado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Bloqueo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        bloqueoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}