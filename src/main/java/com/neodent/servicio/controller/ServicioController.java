package com.neodent.servicio.controller;

import com.neodent.servicio.dto.request.ServicioRequest;
import com.neodent.servicio.dto.response.ServicioResponse;
import com.neodent.servicio.service.ServicioService;
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
@RequestMapping("/api/servicios")
@RequiredArgsConstructor
@Tag(name = "Servicios", description = "Operaciones de gestión de servicios odontológicos")
public class ServicioController {

    private final ServicioService service;

    public record EstadoRequest(
        @NotNull(message = "El estado es obligatorio") Boolean activo
    ) {}



    @Operation(
        summary = "Listar servicios",
        description = "Retorna todos los servicios odontológicos registrados"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicios listados exitosamente")
    })
    @GetMapping
    public List<ServicioResponse> listar() {
        return service.listar();
    }



    @Operation(
        summary = "Obtener servicio por ID",
        description = "Retorna el detalle de un servicio por su identificador"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicio encontrado"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    @GetMapping("/{id}")
    public ServicioResponse obtener(@PathVariable Integer id) {
        return service.obtener(id);
    }



    @Operation(
        summary = "Crear servicio",
        description = "Registra un nuevo servicio odontológico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "El servicio ya existe")
    })
    @PostMapping
    public ResponseEntity<ServicioResponse> crear(@Valid @RequestBody ServicioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }



    @Operation(
        summary = "Actualizar servicio",
        description = "Actualiza la información de un servicio odontológico existente"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicio actualizado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    @PutMapping("/{id}")
    public ServicioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody ServicioRequest request) {
        return service.actualizar(id, request);
    }



    @Operation(
        summary = "Cambiar estado de servicio",
        description = "Activa o desactiva un servicio odontológico"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "404", description = "Servicio no encontrado")
    })
    @PatchMapping("/{id}/estado")
    public ServicioResponse cambiarEstado(@PathVariable Integer id, @Valid @RequestBody EstadoRequest request) {
        return service.cambiarEstado(id, request.activo());
    }
}