package com.neodent.especialidad.controller;

import com.neodent.especialidad.repository.EspecialidadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadRepository repository;

    public record EspecialidadResponse(Integer id, String nombre) {}

    @GetMapping
    public List<EspecialidadResponse> listar() {
        return repository.findByActivoTrueOrderByNombreAsc().stream()
            .map(e -> new EspecialidadResponse(e.getId(), e.getNombre()))
            .toList();
    }
}