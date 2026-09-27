package com.neodent.especialidad.service;

import com.neodent.especialidad.dto.request.EspecialidadRequest;
import com.neodent.especialidad.dto.response.EspecialidadAdminResponse;
import com.neodent.especialidad.model.Especialidad;
import com.neodent.especialidad.repository.EspecialidadRepository;
import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.servicio.repository.ServicioRepository;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadService {

    private final EspecialidadRepository repository;
    private final ServicioRepository servicioRepository;
    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;

    @Transactional(readOnly = true)
    public List<EspecialidadAdminResponse> listar() {
        return repository.findAll(Sort.by("nombre").ascending())
            .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public EspecialidadAdminResponse obtener(Integer id) {
        return mapear(buscar(id));
    }

    @Transactional
    public EspecialidadAdminResponse crear(EspecialidadRequest request) {
        String nombre = request.nombre().trim();
        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una especialidad con ese nombre");
        }

        Especialidad especialidad = new Especialidad();
        actualizarDatos(especialidad, request);
        especialidad.setActivo(true);

        return guardar(especialidad);
    }

    @Transactional
    public EspecialidadAdminResponse actualizar(Integer id, EspecialidadRequest request) {
        Especialidad especialidad = buscar(id);
        String nombre = request.nombre().trim();

        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe otra especialidad con ese nombre");
        }

        actualizarDatos(especialidad, request);
        return guardar(especialidad);
    }

    @Transactional
    public EspecialidadAdminResponse cambiarEstado(Integer id, Boolean activo) {
        Especialidad especialidad = buscar(id);

        if (especialidad.getActivo().equals(activo)) return mapear(especialidad);

        if (!activo) {
            if (servicioRepository.existsByEspecialidadIdAndActivoTrue(id)) {
                throw new ConflictException("No puedes desactivar esta especialidad porque tiene servicios activos");
            }

            if (odontologoEspecialidadRepository.existsByEspecialidadIdAndActivoTrue(id)) {
                throw new ConflictException("No puedes desactivar esta especialidad porque tiene odontólogos asignados");
            }
        }

        especialidad.setActivo(activo);
        return guardar(especialidad);
    }

    private Especialidad buscar(Integer id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Especialidad no encontrada"));
    }

    private void actualizarDatos(Especialidad especialidad, EspecialidadRequest request) {
        especialidad.setNombre(request.nombre().trim());
        especialidad.setDescripcion(request.descripcion() == null || request.descripcion().isBlank()
            ? null : request.descripcion().trim());
    }

    private EspecialidadAdminResponse guardar(Especialidad especialidad) {
        try {
            return mapear(repository.saveAndFlush(especialidad));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("No se pudo guardar la especialidad. Verifica que el nombre no esté duplicado");
        }
    }

    private EspecialidadAdminResponse mapear(Especialidad especialidad) {
        return new EspecialidadAdminResponse(
            especialidad.getId(), especialidad.getNombre(),
            especialidad.getDescripcion(), especialidad.getActivo()
        );
    }
}