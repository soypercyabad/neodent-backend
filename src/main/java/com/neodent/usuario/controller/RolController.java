package com.neodent.usuario.controller;

import com.neodent.usuario.dto.request.RolRequest;
import com.neodent.usuario.dto.response.RolResponse;
import com.neodent.usuario.service.RolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@Tag(name = "Roles", description = "Operaciones de gestión de roles")
public class RolController {
    private final RolService rolService;

    @Operation(
        summary = "Listar roles",
        description = "Retorna todos los roles registrados"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Roles listados exitosamente")
    })
    @GetMapping
    public List<RolResponse> listar(@RequestParam(required = false) Boolean activo) {
        return rolService.listar(activo);
    }



    @Operation(
        summary = "Obtener rol por ID",
        description = "Retorna el detalle de un rol por su identificador"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol encontrado"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado")
    })
    @GetMapping("/{id}")
    public RolResponse obtener(@PathVariable Integer id) {
        return rolService.obtener(id);
    }



    @Operation(
        summary = "Crear rol",
        description = "Registra un nuevo rol"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Rol creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "El rol ya existe")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RolResponse crear(@Valid @RequestBody RolRequest request) {
        return rolService.crear(request);
    }



    @Operation(
        summary = "Actualizar rol",
        description = "Actualiza un rol existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "409", description = "El rol ya existe")
    })
    @PutMapping("/{id}")
    public RolResponse actualizar(
        @PathVariable Integer id,
        @Valid @RequestBody RolRequest request
    ) {
        return rolService.actualizar(id, request);
    }


    
    @Operation(
        summary = "Activar rol",
        description = "Activa un rol existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol activado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "409", description = "El rol ya se encuentra activo")
    })
    @PatchMapping("/{id}/activar")
    public RolResponse activar(@PathVariable Integer id) {
        return rolService.activar(id);
    }

    

    @Operation(
        summary = "Desactivar rol",
        description = "Desactiva un rol existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Rol desactivado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "409", description = "El rol ya se encuentra inactivo o en uso")
    })
    @PatchMapping("/{id}/desactivar")
    public RolResponse desactivar(@PathVariable Integer id) {
        return rolService.desactivar(id);
    }
}