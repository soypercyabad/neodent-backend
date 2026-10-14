package com.neodent.usuario.controller;

import com.neodent.shared.response.PaginaResponse;
import com.neodent.usuario.dto.request.ModuloRequest;
import com.neodent.usuario.dto.response.ModuloResumenEstadosResponse;
import com.neodent.usuario.dto.response.ModuloResponse;
import com.neodent.usuario.service.ModuloService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/modulos")
@RequiredArgsConstructor
@Tag(name = "Módulos", description = "Operaciones de gestión del catálogo de módulos y permisos")
public class ModuloController {

    private final ModuloService moduloService;

    @Operation(summary = "Resumen de conteos de módulos por estado", description = "Retorna el total de módulos, activos e inactivos")
    @GetMapping("/resumen-estados")
    public ModuloResumenEstadosResponse resumenEstados(@RequestParam(required = false) String buscar) {
        return moduloService.resumenEstados(buscar);
    }

    @Operation(summary = "Listar módulos", description = "Retorna el catálogo de módulos del sistema. Si se envía 'page', retorna PaginaResponse con soporte para paginación y búsqueda; si no, retorna la lista completa para compatibilidad.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulos listados exitosamente")
    })
    @GetMapping
    public Object listar(
        @RequestParam(required = false) String buscar,
        @RequestParam(required = false) Boolean activo,
        @RequestParam(required = false) Integer page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String sortBy,
        @RequestParam(defaultValue = "asc") String direction
    ) {
        if (page != null) {
            return PaginaResponse.de(
                moduloService.listarPaginado(buscar, activo, page, size, sortBy, direction)
            );
        }
        return moduloService.listar(activo);
    }

    @Operation(summary = "Obtener módulo por ID", description = "Retorna el detalle de un módulo específico")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo encontrado"),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado")
    })
    @GetMapping("/{id}")
    public ModuloResponse obtener(@PathVariable Integer id) {
        return moduloService.obtener(id);
    }

    @Operation(summary = "Crear módulo", description = "Registra un nuevo módulo en el catálogo")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Módulo creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "El código o la ruta ya están en uso")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ModuloResponse crear(@Valid @RequestBody ModuloRequest request) {
        return moduloService.crear(request);
    }

    @Operation(summary = "Actualizar módulo", description = "Actualiza los datos de un módulo existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado"),
        @ApiResponse(responseCode = "409", description = "El código o la ruta ya están en uso por otro módulo")
    })
    @PutMapping("/{id}")
    public ModuloResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ModuloRequest request) {
        return moduloService.actualizar(id, request);
    }

    @Operation(summary = "Activar módulo", description = "Habilita un módulo inactivo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo activado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado"),
        @ApiResponse(responseCode = "409", description = "El módulo ya se encuentra activo")
    })
    @PatchMapping("/{id}/activar")
    public ModuloResponse activar(@PathVariable Integer id) {
        return moduloService.activar(id);
    }

    @Operation(summary = "Desactivar módulo", description = "Deshabilita un módulo activo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Módulo desactivado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Módulo no encontrado"),
        @ApiResponse(responseCode = "409", description = "El módulo ya se encuentra inactivo")
    })
    @PatchMapping("/{id}/desactivar")
    public ModuloResponse desactivar(@PathVariable Integer id) {
        return moduloService.desactivar(id);
    }
}
