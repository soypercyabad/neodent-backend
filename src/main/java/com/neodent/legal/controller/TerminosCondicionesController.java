package com.neodent.legal.controller;

import com.neodent.legal.dto.AceptacionTerminosResponse;
import com.neodent.legal.dto.AceptarTerminosRequest;
import com.neodent.legal.dto.TerminosCondicionesResponse;
import com.neodent.legal.dto.TerminosPendientesResponse;
import com.neodent.legal.service.TerminosCondicionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/terminos-condiciones")
@RequiredArgsConstructor
@Tag(name = "Términos y Condiciones", description = "Gestión y versionamiento de documentos legales alojados en S3 / R2")
public class TerminosCondicionesController {

    private final TerminosCondicionesService terminosService;

    /* =========================================================
     * ENDPOINTS PÚBLICOS Y USUARIO AUTENTICADO
     * ========================================================= */

    @Operation(
        summary = "Visualizar documento de Términos y Condiciones activo",
        description = "Retorna el flujo binario del PDF activo para apertura directa en el navegador."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PDF obtenido correctamente"),
        @ApiResponse(responseCode = "404", description = "Documento no disponible")
    })
    @GetMapping
    public ResponseEntity<byte[]> verDocumentoActivo() {
        byte[] pdfBytes = terminosService.obtenerPdfActivo();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename(TerminosCondicionesService.NOMBRE_ARCHIVO_DEFECTO).build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(
        summary = "Visualizar documento de Términos y Condiciones por ID",
        description = "Retorna el flujo binario del PDF para visualización nativa en el navegador."
    )
    @GetMapping("/{id}/ver")
    public ResponseEntity<byte[]> verDocumentoPorId(@PathVariable Long id) {
        byte[] pdfBytes = terminosService.obtenerPdfPorId(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("terminos-" + id + ".pdf").build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar metadatos de la versión activa",
        description = "Devuelve el título, versión y URL oficial del documento actualmente vigente."
    )
    @GetMapping("/info")
    public ResponseEntity<TerminosCondicionesResponse> obtenerInfoActivo() {
        return ResponseEntity.ok(terminosService.obtenerInfoActivo());
    }

    @Operation(
        summary = "Verificar si el usuario autenticado tiene términos pendientes por aceptar",
        description = "Revisa si el usuario autenticado ya aceptó la versión oficial activa de los términos y condiciones."
    )
    @GetMapping("/pendiente")
    public ResponseEntity<TerminosPendientesResponse> verificarTerminosPendientes(
        @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt == null || jwt.getSubject() == null) {
            return ResponseEntity.ok(new TerminosPendientesResponse(false, null));
        }
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return ResponseEntity.ok(terminosService.verificarPendiente(usuarioId));
    }

    @Operation(
        summary = "Aceptar los términos y condiciones vigentes",
        description = "Registra la firma digital/aceptación del usuario autenticado para los términos vigentes."
    )
    @PostMapping("/aceptar")
    public ResponseEntity<Map<String, Object>> aceptarTerminos(
        @RequestBody(required = false) AceptarTerminosRequest request,
        @AuthenticationPrincipal Jwt jwt,
        HttpServletRequest httpRequest
    ) {
        if (jwt == null || jwt.getSubject() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long usuarioId = Long.valueOf(jwt.getSubject());
        Long terminosId = request != null ? request.terminosId() : null;
        terminosService.registrarAceptacionUsuario(usuarioId, terminosId, httpRequest);
        return ResponseEntity.ok(Map.of(
            "exito", true,
            "mensaje", "Términos y condiciones aceptados correctamente"
        ));
    }

    /* =========================================================
     * ENDPOINTS ADMINISTRATIVOS (CRUD de versiones en S3)
     * ========================================================= */

    @Operation(
        summary = "Listar todas las versiones de términos y condiciones (Admin)",
        description = "Devuelve el listado de documentos cargados en el sistema. Si se envía 'page', retorna PaginaResponse; si no, la lista completa."
    )
    @GetMapping("/admin")
    public ResponseEntity<?> listarVersiones(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String buscar,
            @RequestParam(required = false) Boolean activo) {
        if (page != null) {
            return ResponseEntity.ok(com.neodent.shared.response.PaginaResponse.de(
                terminosService.listarVersionesPaginado(buscar, activo, page, size)
            ));
        }
        return ResponseEntity.ok(terminosService.listarTodos());
    }

    @Operation(
        summary = "Subir y registrar nueva versión en S3 (Admin)",
        description = "Carga un nuevo archivo PDF a S3/R2 y lo registra con número de versión y título."
    )
    @PostMapping(value = "/admin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TerminosCondicionesResponse> subirVersion(
        @RequestParam("archivo") MultipartFile archivo,
        @RequestParam(value = "titulo", required = false) String titulo,
        @RequestParam(value = "version", required = false, defaultValue = "v1.0") String version,
        @RequestParam(value = "activar", required = false, defaultValue = "false") boolean activar
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(terminosService.subirYCrear(archivo, titulo, version, activar));
    }

    @Operation(
        summary = "Editar versión existente (Admin)",
        description = "Actualiza el título, versión y opcionalmente reemplaza el PDF de una versión registrada."
    )
    @PutMapping(value = "/admin/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TerminosCondicionesResponse> editarVersion(
        @PathVariable Long id,
        @RequestParam(value = "titulo", required = false) String titulo,
        @RequestParam(value = "version", required = false) String version,
        @RequestParam(value = "archivo", required = false) MultipartFile archivo,
        @RequestParam(value = "activar", required = false) Boolean activar
    ) {
        return ResponseEntity.ok(terminosService.editar(id, titulo, version, archivo, activar));
    }

    @Operation(
        summary = "Activar o desactivar versión de términos (Admin)",
        description = "Activa una versión para que sea la oficial utilizada por el sistema."
    )
    @PatchMapping("/admin/{id}/estado")
    public ResponseEntity<TerminosCondicionesResponse> cambiarEstado(
        @PathVariable Long id,
        @RequestBody Map<String, Boolean> body
    ) {
        boolean activo = body.getOrDefault("activo", false);
        return ResponseEntity.ok(terminosService.cambiarEstado(id, activo));
    }

    @Operation(
        summary = "Visualizar versión específica en el navegador (Admin)",
        description = "Abre el PDF en una pestaña del navegador con visualizador nativo."
    )
    @GetMapping("/admin/{id}/ver")
    public ResponseEntity<byte[]> verVersion(@PathVariable Long id) {
        byte[] pdfBytes = terminosService.obtenerPdfPorId(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("terminos-" + id + ".pdf").build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(
        summary = "Eliminar versión de términos y condiciones (Admin)",
        description = "Elimina permanentemente el archivo de S3 y su registro. Solo permitido si no está activa."
    )
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> eliminarVersion(@PathVariable Long id) {
        terminosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Listar personas que aceptaron los términos y condiciones (Admin)",
        description = "Devuelve el registro de auditoría de aceptaciones. Búsqueda por número de documento."
    )
    @GetMapping("/admin/aceptaciones")
    public ResponseEntity<?> listarAceptaciones(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String buscar) {
        if (page != null) {
            return ResponseEntity.ok(com.neodent.shared.response.PaginaResponse.de(
                terminosService.listarAceptacionesPaginado(buscar, page, size)
            ));
        }
        return ResponseEntity.ok(terminosService.listarAceptaciones());
    }
}
