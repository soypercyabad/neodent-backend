package com.neodent.cita.controller;

import com.neodent.cita.dto.request.CambiarEstadoTipoBloqueoRequest;
import com.neodent.cita.dto.request.TipoBloqueoHorarioRequest;
import com.neodent.cita.dto.response.TipoBloqueoHorarioResponse;
import com.neodent.cita.service.TipoBloqueoHorarioService;

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
@RequestMapping("/api/tipos-bloqueo")
@RequiredArgsConstructor
@Tag(
    name = "Tipos de bloqueo",
    description = "Catálogo y administración de tipos de excepciones y bloqueos de agenda"
)
public class TipoBloqueoHorarioController {

    private final TipoBloqueoHorarioService service;



    @Operation(
        summary = "Listar tipos de bloqueo activos",
        description = "Retorna el catálogo de tipos de bloqueo activos disponibles para el registro de excepciones de agenda."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipos de bloqueo activos listados correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping
    public List<TipoBloqueoHorarioResponse> listarActivos() {
        return service.listarActivos();
    }



    @Operation(
        summary = "Listar todos los tipos de bloqueo (Admin)",
        description = "Retorna todos los tipos de bloqueo (activos e inactivos) para la gestión administrativa del catálogo."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Todos los tipos de bloqueo listados correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)")
    })
    @GetMapping("/admin")
    public List<TipoBloqueoHorarioResponse> listarTodos() {
        return service.listarTodos();
    }



    @Operation(
        summary = "Obtener detalle de tipo de bloqueo (Admin)",
        description = "Retorna la información y reglas de configuración de un tipo de bloqueo por su identificador."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de bloqueo obtenido exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Tipo de bloqueo no encontrado")
    })
    @GetMapping("/admin/{id}")
    public TipoBloqueoHorarioResponse obtener(
        @PathVariable Integer id
    ) {
        return service.obtener(id);
    }



    @Operation(
        summary = "Crear tipo de bloqueo",
        description = "Crea un nuevo tipo de bloqueo en el catálogo definiendo sus reglas para odontólogo y sede."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tipo de bloqueo creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas incompatibles"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)"),
        @ApiResponse(responseCode = "409", description = "Código o nombre ya registrados")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TipoBloqueoHorarioResponse crear(
        @Valid @RequestBody TipoBloqueoHorarioRequest request
    ) {
        return service.crear(request);
    }



    @Operation(
        summary = "Actualizar tipo de bloqueo",
        description = "Actualiza el código, nombre, descripción y reglas de un tipo de bloqueo existente."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de bloqueo actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos o reglas incompatibles"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Tipo de bloqueo no encontrado"),
        @ApiResponse(responseCode = "409", description = "Código o nombre ya en uso por otro registro")
    })
    @PutMapping("/{id}")
    public TipoBloqueoHorarioResponse actualizar(
        @PathVariable Integer id,
        @Valid @RequestBody TipoBloqueoHorarioRequest request
    ) {
        return service.actualizar(id, request);
    }



    @Operation(
        summary = "Cambiar estado de tipo de bloqueo",
        description = "Activa o desactiva lógicamente un tipo de bloqueo sin eliminar su historial."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado del tipo de bloqueo actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)"),
        @ApiResponse(responseCode = "404", description = "Tipo de bloqueo no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public TipoBloqueoHorarioResponse cambiarEstado(
        @PathVariable Integer id,
        @Valid @RequestBody CambiarEstadoTipoBloqueoRequest request
    ) {
        return service.cambiarEstado(id, request.activo());
    }
}