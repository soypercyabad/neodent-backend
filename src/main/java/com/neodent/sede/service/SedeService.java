package com.neodent.sede.service;

import com.neodent.sede.dto.request.SedeRequest;
import com.neodent.sede.dto.response.SedeResponse;
import com.neodent.sede.model.Sede;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository repository;

    @Transactional(readOnly = true)
    public List<SedeResponse> listar() {
        return repository.findAll(Sort.by("nombre").ascending())
            .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public SedeResponse obtener(Integer id) {
        return mapear(buscar(id));
    }

    @Transactional
    public SedeResponse crear(SedeRequest request) {
        Sede sede = new Sede();
        actualizarDatos(sede, request);
        sede.setActivo(true);
        return mapear(repository.save(sede));
    }

    @Transactional
    public SedeResponse actualizar(Integer id, SedeRequest request) {
        Sede sede = buscar(id);
        actualizarDatos(sede, request);
        return mapear(repository.save(sede));
    }

    @Transactional
    public SedeResponse cambiarEstado(Integer id, Boolean activo) {
        Sede sede = buscar(id);
        sede.setActivo(activo);
        return mapear(repository.save(sede));
    }

    private Sede buscar(Integer id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
    }

    private void actualizarDatos(Sede sede, SedeRequest request) {
        sede.setNombre(request.nombre().trim());
        sede.setDireccion(request.direccion().trim());
        sede.setDistrito(normalizar(request.distrito()));
        sede.setProvincia(normalizar(request.provincia()));
        sede.setDepartamento(normalizar(request.departamento()));
        sede.setTelefono(normalizar(request.telefono()));
        sede.setEmail(normalizar(request.email()));
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private SedeResponse mapear(Sede sede) {
        return new SedeResponse(
            sede.getId(), sede.getNombre(), sede.getDireccion(),
            sede.getDistrito(), sede.getProvincia(), sede.getDepartamento(),
            sede.getTelefono(), sede.getEmail(), sede.getActivo(),
            sede.getFechaCreacion()
        );
    }
}