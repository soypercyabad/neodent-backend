package com.neodent.usuario.controller;

import com.neodent.usuario.dto.response.ModuloResponse;
import com.neodent.usuario.model.Modulo;
import com.neodent.usuario.repository.ModuloRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/modulos")
@RequiredArgsConstructor
@Tag(name = "Módulos", description = "Catálogo de módulos de navegación y permisos del sistema")
public class ModuloController {

    private final ModuloRepository moduloRepository;

    @Operation(summary = "Listar módulos", description = "Retorna el catálogo de módulos activos para configuración de roles")
    @GetMapping
    public List<ModuloResponse> listar() {
        return moduloRepository.findAllByActivoTrueOrderByOrdenAsc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    private ModuloResponse toResponse(Modulo m) {
        return new ModuloResponse(
            m.getId(),
            m.getCodigo(),
            m.getNombre(),
            m.getDescripcion(),
            m.getRuta(),
            m.getIcono(),
            m.getOrden()
        );
    }
}
