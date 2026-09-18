package com.neodent.dni.controller;

import com.neodent.dni.DniService;
import com.neodent.dni.dto.DniResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dni")
@RequiredArgsConstructor
@Tag(name = "Consulta DNI", description = "Consulta datos de identidad mediante el proveedor externo Apis.net")
public class DniController {

    private final DniService dniService;


    @Operation(
        summary = "Consultar DNI",
        description = """
            Consulta nombres y apellidos asociados a un DNI peruano.
            Este servicio se usa únicamente cuando el paciente todavía no existe en la base de datos de Neodent.
            Si el proveedor externo falla, el flujo de registro debe permitir ingreso manual.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "DNI consultado correctamente"),
        @ApiResponse(responseCode = "400", description = "Formato de DNI inválido"),
        @ApiResponse(responseCode = "404", description = "DNI no encontrado"),
        @ApiResponse(responseCode = "503", description = "Servicio externo no disponible")
    })
    @GetMapping("/{dni}")
    public DniResponse buscarPorDni(
        @Parameter(description = "DNI peruano de 8 dígitos", example = "75195123")
        @PathVariable String dni
    ) {
        return dniService.buscarPorDni(dni);
    }
}