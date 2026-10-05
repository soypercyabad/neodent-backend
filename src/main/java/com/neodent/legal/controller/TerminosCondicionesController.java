package com.neodent.legal.controller;

import com.neodent.legal.dto.TerminosCondicionesResponse;
import com.neodent.legal.service.TerminosCondicionesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
     * ENDPOINTS PÚBLICOS (Para pacientes y registro)
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
        headers.setContentDispositionFormData("inline", TerminosCondicionesService.NOMBRE_ARCHIVO_DEFECTO);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(
        summary = "Consultar metadatos de la versión activa",
        description = "Devuelve el título, versión y tamaño del documento actualmente vigente."
    )
    @GetMapping("/info")
    public ResponseEntity<TerminosCondicionesResponse> obtenerInfoActivo() {
        return ResponseEntity.ok(terminosService.obtenerInfoActivo());
    }

    /* =========================================================
     * ENDPOINTS ADMINISTRATIVOS (CRUD de versiones en S3)
     * ========================================================= */

    @Operation(
        summary = "Listar todas las versiones de términos y condiciones (Admin)",
        description = "Devuelve el listado completo de documentos cargados en el sistema con su estado de activación."
    )
    @GetMapping("/admin")
    public ResponseEntity<List<TerminosCondicionesResponse>> listarVersiones() {
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
        summary = "Activar o desactivar versión de términos (Admin)",
        description = "Activa una versión para que sea la oficial utilizada por el sistema, o la desactiva."
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
        summary = "Descargar o visualizar versión específica (Admin)",
        description = "Obtiene el flujo PDF de una versión específica desde S3."
    )
    @GetMapping("/admin/{id}/descargar")
    public ResponseEntity<byte[]> descargarVersion(@PathVariable Long id) {
        byte[] pdfBytes = terminosService.obtenerPdfPorId(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "terminos-" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @Operation(
        summary = "Eliminar versión de términos y condiciones (Admin)",
        description = "Elimina permanentemente el archivo de S3 y su registro en base de datos."
    )
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> eliminarVersion(@PathVariable Long id) {
        terminosService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
