package com.neodent.catalogo.service;

import com.neodent.catalogo.dto.CatalogoClinicoDto;
import com.neodent.catalogo.model.CatalogoClinico;
import com.neodent.catalogo.model.TipoCatalogoClinico;
import com.neodent.catalogo.repository.CatalogoClinicoRepository;
import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogoClinicoService {

    private final CatalogoClinicoRepository repository;

    @Transactional(readOnly = true)
    public List<CatalogoClinicoDto.Response> listarActivos(TipoCatalogoClinico tipo) {
        List<CatalogoClinico> lista = tipo != null
            ? repository.findByTipoAndActivoTrueOrderByNombreAsc(tipo)
            : repository.findByActivoTrueOrderByNombreAsc();
        return lista.stream().map(this::mapearResponse).toList();
    }

    @Transactional(readOnly = true)
    public CatalogoClinicoDto.CatalogoAgrupadoResponse obtenerCatalogosActivosAgrupados() {
        List<CatalogoClinico> activos = repository.findByActivoTrueOrderByNombreAsc();
        return new CatalogoClinicoDto.CatalogoAgrupadoResponse(
            activos.stream().filter(c -> c.getTipo() == TipoCatalogoClinico.ALERGIA).map(this::mapearResponse).toList(),
            activos.stream().filter(c -> c.getTipo() == TipoCatalogoClinico.PATOLOGIA).map(this::mapearResponse).toList(),
            activos.stream().filter(c -> c.getTipo() == TipoCatalogoClinico.HABITO).map(this::mapearResponse).toList(),
            activos.stream().filter(c -> c.getTipo() == TipoCatalogoClinico.MEDICAMENTO).map(this::mapearResponse).toList()
        );
    }

    @Transactional
    public CatalogoClinicoDto.Response crear(CatalogoClinicoDto.CreateRequest request) {
        if (repository.existsByTipoAndNombreIgnoreCase(request.tipo(), request.nombre().trim())) {
            throw new BusinessException("Ya existe una opción registrada con el nombre '" + request.nombre() + "' en este catálogo.");
        }

        CatalogoClinico nuevo = new CatalogoClinico(
            request.tipo(),
            request.nombre().trim(),
            request.descripcion() != null ? request.descripcion().trim() : null,
            request.activo() != null ? request.activo() : true
        );

        CatalogoClinico guardado = repository.save(nuevo);
        log.info("Nueva opción de catálogo creada: [{}] {}", guardado.getTipo(), guardado.getNombre());
        return mapearResponse(guardado);
    }

    @Transactional
    public CatalogoClinicoDto.Response toggleActivo(Integer id) {
        CatalogoClinico item = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Opción de catálogo no encontrada con ID: " + id));

        item.setActivo(!Boolean.TRUE.equals(item.getActivo()));
        CatalogoClinico actualizado = repository.save(item);
        log.info("Estado de opción de catálogo ID {} cambiado a activo: {}", id, actualizado.getActivo());
        return mapearResponse(actualizado);
    }

    private CatalogoClinicoDto.Response mapearResponse(CatalogoClinico c) {
        return new CatalogoClinicoDto.Response(
            c.getId(),
            c.getTipo(),
            c.getNombre(),
            c.getDescripcion(),
            c.getActivo()
        );
    }
}
