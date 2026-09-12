package com.neodent.paciente.controller;

import com.neodent.paciente.dto.ActualizarPacienteRequest;
import com.neodent.paciente.dto.CrearPacienteRequest;
import com.neodent.paciente.dto.PacienteResponse;
import com.neodent.paciente.service.PacienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(
    name = "Pacientes",
    description = "Operaciones para consulta, registro, actualización y gestión del estado de pacientes"
)
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteService pacienteService;


    @Operation(
        summary = "Buscar paciente por ID",
        description = "Obtiene la información de un paciente registrado usando su identificador interno."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Paciente encontrado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Paciente no encontrado"
        )
    })
    @GetMapping("/{id}")
    public PacienteResponse buscarPorId(
        @Parameter(
            description = "ID interno del paciente",
            example = "1"
        )
        @PathVariable Long id
    ) {
        return pacienteService.buscarPorId(id);
    }


    @Operation(
        summary = "Buscar paciente por documento",
        description = """
            Busca un paciente previamente registrado usando tipo y número de documento.
            Este endpoint debe consultarse antes de llamar al servicio externo de DNI.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Paciente encontrado"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Paciente no registrado"
        )
    })
    @GetMapping("/documento/{tipoDocumento}/{numeroDocumento}")
    public PacienteResponse buscarPorDocumento(

        @Parameter(
            description = "Código del tipo de documento",
            example = "DNI"
        )
        @PathVariable String tipoDocumento,

        @Parameter(
            description = "Número de documento del paciente",
            example = "71234567"
        )
        @PathVariable String numeroDocumento
    ) {

        return pacienteService.buscarPorDocumento(
            tipoDocumento,
            numeroDocumento
        );
    }


    @Operation(
        summary = "Registrar paciente",
        description = """
            Registra un nuevo paciente en Neodent.

            El paciente puede existir sin una cuenta de usuario.
            En ese caso, usuario_id permanece NULL hasta que posteriormente
            active o cree su cuenta del portal.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Paciente registrado correctamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Ya existe un paciente con el mismo documento"
        )
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse crear(
        @Valid @RequestBody CrearPacienteRequest request
    ) {

        return pacienteService.crear(request);
    }


    @Operation(
        summary = "Actualizar datos del paciente",
        description = """
            Actualiza los datos administrativos permitidos de un paciente.

            No modifica:
            - ID del paciente
            - tipo de documento
            - número de documento
            - usuario asociado
            - estado activo/inactivo
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Paciente actualizado correctamente"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Datos inválidos"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Paciente no encontrado"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "No se puede modificar un paciente inactivo"
        )
    })
    @PutMapping("/{id}")
    public PacienteResponse actualizar(

        @Parameter(
            description = "ID interno del paciente",
            example = "1"
        )
        @PathVariable Long id,

        @Valid
        @RequestBody ActualizarPacienteRequest request
    ) {

        return pacienteService.actualizar(
            id,
            request
        );
    }


    @Operation(
        summary = "Desactivar paciente",
        description = """
            Realiza una desactivación lógica del paciente.

            No elimina físicamente el registro ni sus citas,
            historia clínica, recetas o documentos relacionados.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Paciente desactivado correctamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Paciente no encontrado"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El paciente ya se encuentra inactivo"
        )
    })
    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(
        @Parameter(
            description = "ID interno del paciente",
            example = "1"
        )
        @PathVariable Long id
    ) {

        pacienteService.desactivar(id);
    }


    @Operation(
        summary = "Reactivar paciente",
        description = "Reactiva un paciente previamente desactivado."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Paciente activado correctamente"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Paciente no encontrado"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "El paciente ya se encuentra activo"
        )
    })
    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(
        @Parameter(
            description = "ID interno del paciente",
            example = "1"
        )
        @PathVariable Long id
    ) {

        pacienteService.activar(id);
    }
}