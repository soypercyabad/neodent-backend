package com.neodent.historia.controller;

import com.neodent.historia.dto.EvolucionClinicaDto;
import com.neodent.historia.dto.GuardarHistoriaClinicaRequest;
import com.neodent.historia.dto.HistoriaClinicaResponse;
import com.neodent.historia.service.HistoriaClinicaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes/{pacienteId}/historia-clinica")
@RequiredArgsConstructor
@Tag(name = "Historia Clínica", description = "Gestión de historias clínicas y antecedentes de pacientes")
public class HistoriaClinicaController {

    private final HistoriaClinicaService historiaService;

    @Operation(summary = "Consultar historia clínica del paciente")
    @GetMapping
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public ResponseEntity<HistoriaClinicaResponse> obtenerHistoriaClinica(@PathVariable Long pacienteId) {
        return historiaService.obtenerPorPacienteId(pacienteId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Crear historia clínica inicial para un paciente")
    @PostMapping
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'PACIENTE')")
    public ResponseEntity<HistoriaClinicaResponse> crearHistoriaClinica(
        @PathVariable Long pacienteId,
        @Valid @RequestBody GuardarHistoriaClinicaRequest request
    ) {
        HistoriaClinicaResponse response = historiaService.crearHistoriaClinica(pacienteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Actualizar antecedentes y datos de la historia clínica")
    @PutMapping
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'PACIENTE')")
    public ResponseEntity<HistoriaClinicaResponse> actualizarHistoriaClinica(
        @PathVariable Long pacienteId,
        @Valid @RequestBody GuardarHistoriaClinicaRequest request
    ) {
        HistoriaClinicaResponse response = historiaService.actualizarHistoriaClinica(pacienteId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Listar notas de evolución clínica del paciente")
    @GetMapping("/evoluciones")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public ResponseEntity<List<EvolucionClinicaDto.Response>> listarEvoluciones(@PathVariable Long pacienteId) {
        return ResponseEntity.ok(historiaService.listarEvoluciones(pacienteId));
    }

    @Operation(summary = "Registrar nueva nota de evolución clínica")
    @PostMapping("/evoluciones")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<EvolucionClinicaDto.Response> agregarEvolucion(
        @PathVariable Long pacienteId,
        @Valid @RequestBody EvolucionClinicaDto.Request request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        EvolucionClinicaDto.Response response = historiaService.agregarEvolucion(pacienteId, request, usuarioId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
