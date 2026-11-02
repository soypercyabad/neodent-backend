package com.neodent.catalogo.controller;

import com.neodent.catalogo.dto.CatalogoClinicoDto;
import com.neodent.catalogo.model.TipoCatalogoClinico;
import com.neodent.catalogo.service.CatalogoClinicoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalogos-clinicos")
@RequiredArgsConstructor
@Tag(name = "Catálogos Clínicos", description = "Gestión de opciones para historia clínica (alergias, patologías, hábitos, medicamentos)")
public class CatalogoClinicoController {

    private final CatalogoClinicoService service;

    @Operation(summary = "Obtener catálogos activos agrupados (alergias, patologías, hábitos, medicamentos)")
    @GetMapping("/activos")
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public ResponseEntity<CatalogoClinicoDto.CatalogoAgrupadoResponse> obtenerActivosAgrupados() {
        return ResponseEntity.ok(service.obtenerCatalogosActivosAgrupados());
    }

    @Operation(summary = "Listar opciones activas con filtro opcional por tipo")
    @GetMapping
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN', 'RECEPCIONISTA', 'PACIENTE')")
    public ResponseEntity<List<CatalogoClinicoDto.Response>> listarActivos(
        @RequestParam(required = false) TipoCatalogoClinico tipo
    ) {
        return ResponseEntity.ok(service.listarActivos(tipo));
    }

    @Operation(summary = "Crear nueva opción de catálogo (solo Odontólogo o Admin)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ODONTOLOGO', 'ADMIN')")
    public ResponseEntity<CatalogoClinicoDto.Response> crear(
        @Valid @RequestBody CatalogoClinicoDto.CreateRequest request
    ) {
        CatalogoClinicoDto.Response response = service.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Activar o desactivar una opción de catálogo (solo Admin)")
    @PatchMapping("/{id}/toggle-activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CatalogoClinicoDto.Response> toggleActivo(@PathVariable Integer id) {
        return ResponseEntity.ok(service.toggleActivo(id));
    }
}
