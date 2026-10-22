package com.neodent.odontograma.controller;

import com.neodent.odontograma.dto.ActualizarPiezaRequest;
import com.neodent.odontograma.dto.CondicionOdontogramaResponse;
import com.neodent.odontograma.dto.OdontogramaResponse;
import com.neodent.odontograma.service.OdontogramaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Odontograma", description = "Gestión de odontogramas y estado de piezas dentales")
public class OdontogramaController {

    private final OdontogramaService odontogramaService;

    @Operation(summary = "Catálogo de condiciones clínicas del odontograma")
    @GetMapping("/odontograma/condiciones")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public ResponseEntity<List<CondicionOdontogramaResponse>> listarCondiciones() {
        return ResponseEntity.ok(odontogramaService.listarCondiciones());
    }

    @Operation(summary = "Crear nueva condición clínica o tratamiento personalizado")
    @PostMapping("/odontograma/condiciones")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<CondicionOdontogramaResponse> crearCondicion(
        @Valid @RequestBody com.neodent.odontograma.dto.CrearCondicionRequest request
    ) {
        CondicionOdontogramaResponse response = odontogramaService.crearCondicion(request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Consultar el odontograma actual del paciente")
    @GetMapping("/pacientes/{pacienteId}/odontograma")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public ResponseEntity<OdontogramaResponse> obtenerOdontograma(
        @PathVariable Long pacienteId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        return ResponseEntity.ok(odontogramaService.obtenerOdontogramaActual(pacienteId, usuarioId));
    }

    @Operation(summary = "Actualizar estado de una pieza dental o superficie en el odontograma")
    @PostMapping("/pacientes/{pacienteId}/odontograma/pieza")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<OdontogramaResponse> actualizarPieza(
        @PathVariable Long pacienteId,
        @Valid @RequestBody ActualizarPiezaRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = jwt != null ? Long.valueOf(jwt.getSubject()) : null;
        OdontogramaResponse response = odontogramaService.actualizarPieza(pacienteId, request, usuarioId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Limpiar hallazgos en una pieza dental o superficie específica")
    @DeleteMapping("/pacientes/{pacienteId}/odontograma/pieza/{numeroPieza}")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<OdontogramaResponse> limpiarPieza(
        @PathVariable Long pacienteId,
        @PathVariable Integer numeroPieza,
        @RequestParam(required = false) String superficie
    ) {
        OdontogramaResponse response = odontogramaService.limpiarPieza(pacienteId, numeroPieza, superficie);
        return ResponseEntity.ok(response);
    }
}
