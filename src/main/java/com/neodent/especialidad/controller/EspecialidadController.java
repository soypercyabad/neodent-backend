package com.neodent.especialidad.controller;

import com.neodent.especialidad.dto.request.EspecialidadRequest;
import com.neodent.especialidad.dto.response.EspecialidadAdminResponse;
import com.neodent.especialidad.repository.EspecialidadRepository;
import com.neodent.especialidad.service.EspecialidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
@Tag(name = "Especialidades", description = "Operaciones de gestión de especialidades")
public class EspecialidadController {

    private final EspecialidadRepository repository;
    private final EspecialidadService service;

    public record EspecialidadResponse(Integer id, String nombre) {}

    public record EstadoRequest(
        @NotNull(message = "El estado es obligatorio") Boolean activo
    ) {}



    @Operation(
        summary = "Listar especialidades activas",
        description = "Catálogo de especialidades activas para el sistema"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Especialidades activas listadas exitosamente")
    })
    @GetMapping
    public List<EspecialidadResponse> listar() {
        return repository.findByActivoTrueOrderByNombreAsc().stream()
            .map(e -> new EspecialidadResponse(e.getId(), e.getNombre()))
            .toList();
    }



    @Operation(
        summary = "Listar todas las especialidades",
        description = "Administración: lista todas las especialidades registradas incluyendo inactivas"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Especialidades listadas exitosamente")
    })
    @GetMapping("/admin")
    public List<EspecialidadAdminResponse> listarTodas() {
        return service.listar();
    }



    @Operation(
        summary = "Obtener especialidad por ID",
        description = "Retorna el detalle administrativo de una especialidad por su identificador"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Especialidad encontrada"),
        @ApiResponse(responseCode = "404", description = "Especialidad no encontrada")
    })
    @GetMapping("/admin/{id}")
    public EspecialidadAdminResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }



    @Operation(
        summary = "Crear especialidad",
        description = "Registra una nueva especialidad"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Especialidad creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "La especialidad ya existe")
    })
    @PostMapping
    public ResponseEntity<EspecialidadAdminResponse> crear(@Valid @RequestBody EspecialidadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }



    @Operation(
        summary = "Actualizar especialidad",
        description = "Actualiza los datos de una especialidad existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Especialidad actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Especialidad no encontrada")
    })
    @PutMapping("/{id}")
    public EspecialidadAdminResponse actualizar(
        @PathVariable Integer id,
        @Valid @RequestBody EspecialidadRequest request
    ) {
        return service.actualizar(id, request);
    }



    @Operation(
        summary = "Cambiar estado de especialidad",
        description = "Activa o desactiva una especialidad"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Especialidad no encontrada")
    })
    @PatchMapping("/{id}/estado")
    public EspecialidadAdminResponse cambiarEstado(
        @PathVariable Integer id,
        @Valid @RequestBody EstadoRequest request
    ) {
        return service.cambiarEstado(id, request.activo());
    }
}