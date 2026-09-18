package com.neodent.usuario.controller;

import com.neodent.shared.response.PaginaResponse;
import com.neodent.usuario.dto.request.ActualizarUsuarioInternoRequest;
import com.neodent.usuario.dto.request.CrearUsuarioInternoRequest;
import com.neodent.usuario.dto.request.VerificarDocumentoPersonalRequest;
import com.neodent.usuario.dto.response.UsuarioInternoResponse;
import com.neodent.usuario.dto.response.VerificarDocumentoPersonalResponse;
import com.neodent.usuario.service.UsuarioInternoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios-internos")
@RequiredArgsConstructor
@Tag(
    name = "Usuarios internos",
    description = "Administración de cuentas de personal interno de NeoDent"
)
public class UsuarioInternoController {

    private final UsuarioInternoService usuarioInternoService;

    @Operation(
        summary = "Crear usuario interno",
        description = "Crea un usuario interno y su perfil de personal. Si posee rol ODONTOLOGO también crea su perfil profesional y especialidades."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario interno creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "Correo, documento o colegiatura ya registrados")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioInternoResponse crear(
        @Valid @RequestBody CrearUsuarioInternoRequest request
    ) {
        return usuarioInternoService.crear(request);
    }



    @Operation(
        summary = "Listar usuarios internos",
        description = "Lista usuarios internos con paginación y filtros opcionales por rol, estado, activo y búsqueda."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuarios internos listados exitosamente"),
        @ApiResponse(responseCode = "400", description = "Parámetros de paginación o filtros inválidos")
    })
    @GetMapping
    public PaginaResponse<UsuarioInternoResponse> listar(
        @RequestParam(required = false) String rol,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) Boolean activo,
        @RequestParam(required = false) String buscar,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "desc") String direction
    ) {
        List<String> camposOrdenPermitidos = List.of(
            "id",
            "correo",
            "aliasInterno",
            "fechaCreacion"
        );

        if (!camposOrdenPermitidos.contains(sortBy)) {
            sortBy = "id";
        }

        Sort.Direction sortDirection =
            "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(sortDirection, sortBy)
        );

        Page<UsuarioInternoResponse> resultado =
            usuarioInternoService.listar(
                rol,
                estado,
                activo,
                buscar,
                pageable
            );

        return PaginaResponse.de(resultado);
    }



    @Operation(
        summary = "Obtener usuario interno",
        description = "Obtiene un usuario interno por ID."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario interno obtenido exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario interno no encontrado")
    })
    @GetMapping("/{id}")
    public UsuarioInternoResponse obtener(@PathVariable Long id) {
        return usuarioInternoService.obtener(id);
    }


    
    @Operation(
        summary = "Actualizar usuario interno",
        description = "Actualiza los datos, roles y perfil profesional de un usuario interno."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario interno actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario interno no encontrado"),
        @ApiResponse(responseCode = "409", description = "Correo, documento o colegiatura ya registrados")
    })
    @PutMapping("/{id}")
    public UsuarioInternoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ActualizarUsuarioInternoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioAutenticadoId = Long.valueOf(jwt.getSubject());
        return usuarioInternoService.actualizar(id, request, usuarioAutenticadoId);
    }



    @Operation(
        summary = "Activar usuario interno",
        description = "Activa nuevamente la cuenta y perfil del personal."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario interno activado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario interno no encontrado")
    })
    @PatchMapping("/{id}/activar")
    public UsuarioInternoResponse activar(@PathVariable Long id) {
        return usuarioInternoService.activar(id);
    }


    
    @Operation(
        summary = "Desactivar usuario interno",
        description = "Desactiva la cuenta del usuario, el personal asociado y revoca sus sesiones activas."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario interno desactivado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario interno no encontrado")
    })
    @PatchMapping("/{id}/desactivar")
    public UsuarioInternoResponse desactivar(
        @PathVariable Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioAutenticadoId = Long.valueOf(jwt.getSubject());
        return usuarioInternoService.desactivar(id, usuarioAutenticadoId);
    }


    
    @Operation(
        summary = "Verificar documento personal",
        description = "Verifica si un documento se encuentra disponible y si se puede obtener datos desde el DNI."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Documento verificado exitosamente"),
        @ApiResponse(responseCode = "409", description = "Documento ya registrado como personal")
    })
    @PostMapping("/check-documento")
    public VerificarDocumentoPersonalResponse verificarDocumento(
        @Valid @RequestBody VerificarDocumentoPersonalRequest request
    ) {
        return usuarioInternoService.verificarDocumento(
            request.tipoDocumentoId(),
            request.numeroDocumento()
        );
    }
}