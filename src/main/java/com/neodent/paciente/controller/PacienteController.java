package com.neodent.paciente.controller;

import com.neodent.auth.service.AccountInvitationService;
import com.neodent.paciente.dto.ActualizarPacienteRequest;
import com.neodent.paciente.dto.CrearPacienteRequest;
import com.neodent.paciente.dto.PacienteResponse;
import com.neodent.paciente.service.PacienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.neodent.shared.response.PaginaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
@RequiredArgsConstructor
@Tag(name = "Pacientes", description = "Operaciones para consulta, registro, actualización y gestión del estado de pacientes")
@SecurityRequirement(name = "bearerAuth")
public class PacienteController {

    private final PacienteService pacienteService;
    private final AccountInvitationService accountInvitationService;

    @Operation(
        summary = "Buscar paciente por ID", 
        description = "Obtiene la información de un paciente registrado usando su identificador interno."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente encontrado"),
            @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    })
    @GetMapping("/{id}")
    public PacienteResponse buscarPorId(
            @Parameter(description = "ID interno del paciente", example = "1") @PathVariable Long id) {
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
            @ApiResponse(responseCode = "200", description = "Paciente encontrado"),
            @ApiResponse(responseCode = "404", description = "Paciente no registrado")
    })
    @GetMapping("/documento/{tipoDocumento}/{numeroDocumento}")
    public PacienteResponse buscarPorDocumento(
            @Parameter(description = "Código del tipo de documento", example = "DNI") @PathVariable String tipoDocumento,
            @Parameter(description = "Número de documento del paciente", example = "71234567") @PathVariable String numeroDocumento) {
        return pacienteService.buscarPorDocumento(tipoDocumento, numeroDocumento);
    }



    @Operation(
        summary = "Registrar paciente", 
        description = """
            Registra un nuevo paciente en Neodent.
            El paciente puede existir sin una cuenta de usuario.
            En ese caso, usuario_id permanece NULL hasta que posteriormente active o cree su cuenta del portal.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Paciente registrado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "409", description = "Ya existe un paciente con el mismo documento")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PacienteResponse crear(@Valid @RequestBody CrearPacienteRequest request) {
        return pacienteService.crear(request);
    }



    @Operation(
        summary = "Actualizar datos del paciente", 
        description = """
            Actualiza los datos administrativos permitidos de un paciente.
            No modifica: ID, tipo/número de documento, usuario asociado ni estado activo/inactivo.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paciente actualizado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Paciente no encontrado"),
            @ApiResponse(responseCode = "409", description = "No se puede modificar un paciente inactivo")
    })
    @PutMapping("/{id}")
    public PacienteResponse actualizar(
            @Parameter(description = "ID interno del paciente", example = "1") @PathVariable Long id,
            @Valid @RequestBody ActualizarPacienteRequest request) {
        return pacienteService.actualizar(id, request);
    }



    @Operation(
        summary = "Desactivar paciente", 
        description = """
            Realiza una desactivación lógica del paciente.
            No elimina físicamente el registro ni sus citas, historia clínica, recetas o documentos relacionados.
            """)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente desactivado correctamente"),
            @ApiResponse(responseCode = "404", description = "Paciente no encontrado"),
            @ApiResponse(responseCode = "409", description = "El paciente ya se encuentra inactivo")
    })
    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@Parameter(description = "ID interno del paciente", example = "1") @PathVariable Long id) {
        pacienteService.desactivar(id);
    }



    @Operation(
        summary = "Reactivar paciente", 
        description = "Reactiva un paciente previamente desactivado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Paciente activado correctamente"),
            @ApiResponse(responseCode = "404", description = "Paciente no encontrado"),
            @ApiResponse(responseCode = "409", description = "El paciente ya se encuentra activo")
    })
    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@Parameter(description = "ID interno del paciente", example = "1") @PathVariable Long id) {
        pacienteService.activar(id);
    }



    @Operation(
        summary = "Reenviar invitación para crear cuenta",
        description = """
            Genera una nueva invitación para un paciente que todavía no tiene cuenta.
            Las invitaciones anteriores son revocadas y el nuevo enlace se envía
            al correo registrado del paciente.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invitación enviada correctamente"),
        @ApiResponse(responseCode = "404", description = "Paciente no encontrado"),
        @ApiResponse(responseCode = "409", description = "Paciente inactivo, sin correo o con cuenta ya creada")
    })
    @PostMapping("/{id}/reenviar-invitacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reenviarInvitacion(
        @Parameter(description = "ID interno del paciente", example = "1")
        @PathVariable Long id
    ) {
        accountInvitationService.reenviarInvitacion(id);
    }



    @Operation(
        summary = "Listar pacientes", 
        description = "Lista pacientes con paginación y filtros por estado, existencia de cuenta y búsqueda general."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pacientes listados correctamente")
    })
    @GetMapping
    public PaginaResponse<PacienteResponse> listar(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Boolean conCuenta,
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        List<String> camposOrdenPermitidos = List.of(
                "id",
                "nombres",
                "apellidoPaterno",
                "numeroDocumento",
                "fechaCreacion");

        if (!camposOrdenPermitidos.contains(sortBy)) {
            sortBy = "id";
        }

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy));

        Page<PacienteResponse> resultado = pacienteService.listar(
                activo,
                conCuenta,
                buscar,
                pageable);

        return PaginaResponse.de(resultado);
    }
}