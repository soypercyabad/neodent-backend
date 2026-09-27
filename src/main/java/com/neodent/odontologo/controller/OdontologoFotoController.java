package com.neodent.odontologo.controller;

import com.neodent.odontologo.service.OdontologoFotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/odontologos")
@RequiredArgsConstructor
@Tag(name = "Odontólogos - Fotografía", description = "Operaciones de consulta y visualización de fotografía de perfil de odontólogos")
public class OdontologoFotoController {

    private final OdontologoFotoService odontologoFotoService;



    @Operation(
        summary = "Obtener fotografía de odontólogo",
        description = "Retorna el archivo binario de la fotografía del odontólogo en formato WebP con cabeceras de caché"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Fotografía obtenida correctamente"),
        @ApiResponse(responseCode = "404", description = "Odontólogo o fotografía no encontrada")
    })
    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> obtenerFoto(
        @Parameter(description = "ID del odontólogo", example = "1")
        @PathVariable Long id
    ) {
        var foto = odontologoFotoService.obtener(id);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(foto.contentType()))
            .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
            .body(foto.contenido());
    }
}