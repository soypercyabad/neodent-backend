package com.neodent.auth.controller;

import com.neodent.paciente.dto.TipoDocumentoResponse;
import com.neodent.paciente.repository.TipoDocumentoRepository;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/auth/tipos-documento")
@RequiredArgsConstructor
@Tag(name = "Tipos de documento", description = "Endpoints públicos para tipos de documento")
public class TipoDocumentoPublicController {
    private final TipoDocumentoRepository tipoDocumentoRepository;

    @GetMapping
    public List<TipoDocumentoResponse> listar() {
        return tipoDocumentoRepository.findAllByActivoTrueOrderByIdAsc().stream()
            .map(t -> new TipoDocumentoResponse(t.getId(), t.getCodigo(), t.getNombre(), t.getLongitudMin(), t.getLongitudMax()))
            .toList();
    }
}