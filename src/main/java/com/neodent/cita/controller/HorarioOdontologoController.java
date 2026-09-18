package com.neodent.cita.controller;

import com.neodent.cita.dto.request.ActualizarHorarioOdontologoRequest;
import com.neodent.cita.dto.request.CrearHorarioOdontologoRequest;
import com.neodent.cita.dto.response.HorarioOdontologoResponse;
import com.neodent.cita.service.HorarioOdontologoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horarios")
@RequiredArgsConstructor
@Tag(name = "Horarios", description = "Gestión de horarios de atención odontológica")
public class HorarioOdontologoController {

    private final HorarioOdontologoService horarioService;

    @Operation(
        summary = "Listar horarios de odontólogo",
        description = "Retorna todos los horarios de atención de un odontólogo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Horarios obtenidos correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping
    public List<HorarioOdontologoResponse> listar(
        @RequestParam(required = false) Long odontologoEspecialidadId
    ) {
        return horarioService.listar(odontologoEspecialidadId);
    }



    @Operation(
        summary = "Crear horario de odontólogo",
        description = "Crea un horario de atención para un odontólogo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Horario creado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Odontólogo o sede no encontrado")
    })
    @PostMapping
    public HorarioOdontologoResponse crear(
        @Valid @RequestBody CrearHorarioOdontologoRequest request
    ) {
        return horarioService.crear(request);
    }



    @Operation(
        summary = "Actualizar horario de odontólogo",
        description = "Actualiza un horario de atención para un odontólogo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Horario actualizado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Horario no encontrado")
    })
    @PutMapping("/{id}")
    public HorarioOdontologoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarHorarioOdontologoRequest request
    ) {
        return horarioService.actualizar(id, request);
    }



    @Operation(
        summary = "Eliminar horario de odontólogo",
        description = "Elimina un horario de atención de un odontólogo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Horario eliminado correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "404", description = "Horario no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        horarioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}