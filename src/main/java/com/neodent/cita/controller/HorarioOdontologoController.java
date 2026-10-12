package com.neodent.cita.controller;

import com.neodent.cita.dto.request.ActualizarHorarioOdontologoRequest;
import com.neodent.cita.dto.request.CrearHorarioOdontologoRequest;
import com.neodent.cita.dto.response.HorarioOdontologoCatalogoResponse;
import com.neodent.cita.dto.response.HorarioOdontologoResponse;
import com.neodent.cita.dto.response.HorarioResumenEstadosResponse;
import com.neodent.cita.service.HorarioOdontologoService;
import com.neodent.shared.response.PaginaResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
        description = "Retorna horarios de atención con soporte de paginación o listado completo"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Horarios obtenidos correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping
    public ResponseEntity<?> listar(
        @RequestParam(required = false) Long odontologoEspecialidadId,
        @RequestParam(required = false) Long odontologoId,
        @RequestParam(required = false) Integer especialidadId,
        @RequestParam(required = false) Integer sedeId,
        @RequestParam(required = false) Byte diaSemana,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) String buscar,
        @RequestParam(required = false) Integer page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        if (page != null) {
            Page<HorarioOdontologoResponse> resultado = horarioService.listarPaginado(
                odontologoEspecialidadId,
                odontologoId,
                especialidadId,
                sedeId,
                diaSemana,
                estado,
                buscar,
                page,
                size,
                sortBy,
                direction
            );
            return ResponseEntity.ok(PaginaResponse.de(resultado));
        }

        return ResponseEntity.ok(horarioService.listar(odontologoEspecialidadId, odontologoId));
    }



    @Operation(
        summary = "Resumen de conteos por estado de horarios",
        description = "Retorna el conteo total, vigentes, próximos y expirados según los filtros activos"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumen obtenido correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/resumen-estados")
    public HorarioResumenEstadosResponse resumenEstados(
        @RequestParam(required = false) Long odontologoEspecialidadId,
        @RequestParam(required = false) Long odontologoId,
        @RequestParam(required = false) Integer especialidadId,
        @RequestParam(required = false) Integer sedeId,
        @RequestParam(required = false) Byte diaSemana,
        @RequestParam(required = false) String buscar
    ) {
        return horarioService.resumenEstados(
            odontologoEspecialidadId,
            odontologoId,
            especialidadId,
            sedeId,
            diaSemana,
            buscar
        );
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



    @Operation(
        summary = "Listar odontólogos y especialidades para horarios",
        description = "Retorna las relaciones activas odontólogo-especialidad disponibles para configurar horarios"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Odontólogos y especialidades obtenidos correctamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/catalogo-odontologos")
    public List<HorarioOdontologoCatalogoResponse> listarCatalogoOdontologos() {
        return horarioService.listarCatalogoOdontologos();
    }
}