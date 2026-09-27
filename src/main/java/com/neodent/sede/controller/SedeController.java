package com.neodent.sede.controller;

import com.neodent.sede.dto.request.SedeRequest;
import com.neodent.sede.dto.response.SedeResponse;
import com.neodent.sede.service.SedeService;
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
@RequestMapping("/api/sedes")
@RequiredArgsConstructor
@Tag(name = "Sedes", description = "Operaciones de gestión de sedes")
public class SedeController {

    private final SedeService service;

    public record EstadoRequest(@NotNull(message = "El estado es obligatorio") Boolean activo) {}



    @Operation(
        summary = "Listar sedes",
        description = "Retorna la lista de todas las sedes registradas"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sedes listadas exitosamente")
    })
    @GetMapping
    public List<SedeResponse> listar() {
        return service.listar();
    }



    @Operation(
        summary = "Obtener sede por ID",
        description = "Retorna el detalle de una sede por su identificador"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sede encontrada"),
        @ApiResponse(responseCode = "404", description = "Sede no encontrada")
    })
    @GetMapping("/{id}")
    public SedeResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }



    @Operation(
        summary = "Crear sede",
        description = "Registra una nueva sede"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sede creada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "La sede ya existe")
    })
    @PostMapping
    public ResponseEntity<SedeResponse> crear(@Valid @RequestBody SedeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }



    @Operation(
        summary = "Actualizar sede",
        description = "Actualiza la información de una sede existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sede actualizada exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Sede no encontrada")
    })
    @PutMapping("/{id}")
    public SedeResponse actualizar(@PathVariable Integer id, @Valid @RequestBody SedeRequest request) {
        return service.actualizar(id, request);
    }



    @Operation(
        summary = "Cambiar estado de sede",
        description = "Activa o desactiva una sede"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Sede no encontrada")
    })
    @PatchMapping("/{id}/estado")
    public SedeResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.activo());
    }
}