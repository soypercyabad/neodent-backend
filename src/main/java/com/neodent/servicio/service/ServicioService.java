package com.neodent.servicio.service;

import com.neodent.especialidad.model.Especialidad;
import com.neodent.especialidad.repository.EspecialidadRepository;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.servicio.dto.request.ServicioRequest;
import com.neodent.servicio.dto.response.ServicioResponse;
import com.neodent.servicio.model.Servicio;
import com.neodent.servicio.model.ServicioSede;
import com.neodent.servicio.repository.ServicioRepository;
import com.neodent.servicio.repository.ServicioSedeRepository;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServicioService {

    private final ServicioRepository repository;
    private final ServicioSedeRepository servicioSedeRepository;
    private final EspecialidadRepository especialidadRepository;
    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar() {
        return repository.findAll(Sort.by("nombre").ascending())
            .stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(Integer id) {
        return mapear(buscar(id));
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest request) {
        Especialidad especialidad = buscarEspecialidad(request.especialidadId());
        validarNombre(request.especialidadId(), request.nombre().trim(), null);
        validarSedes(request.sedeIds());

        Servicio servicio = new Servicio();
        servicio.setEspecialidad(especialidad);
        servicio.setActivo(true);
        actualizarDatos(servicio, request);

        Servicio guardado = repository.saveAndFlush(servicio);
        actualizarSedes(guardado.getId(), request.sedeIds());

        return mapear(guardado);
    }

    @Transactional
    public ServicioResponse actualizar(Integer id, ServicioRequest request) {
        Servicio servicio = buscar(id);
        Especialidad especialidad = buscarEspecialidad(request.especialidadId());

        if (!servicio.getEspecialidad().getId().equals(especialidad.getId())) {
            throw new ConflictException(
                "La especialidad de un servicio existente no puede modificarse. " +
                "Registra un nuevo servicio si necesitas asociarlo a otra especialidad"
            );
        }

        validarNombre(request.especialidadId(), request.nombre().trim(), id);
        validarSedes(request.sedeIds());

        actualizarDatos(servicio, request);

        Servicio guardado = repository.saveAndFlush(servicio);
        actualizarSedes(guardado.getId(), request.sedeIds());

        return mapear(guardado);
    }

    @Transactional
    public ServicioResponse cambiarEstado(Integer id, Boolean activo) {
        Servicio servicio = buscar(id);

        if (activo && !Boolean.TRUE.equals(servicio.getEspecialidad().getActivo())) {
            throw new ConflictException(
                "No puedes activar un servicio cuya especialidad está inactiva"
            );
        }

        servicio.setActivo(activo);
        return mapear(repository.saveAndFlush(servicio));
    }

    @Transactional
    public ServicioResponse alternarDestacado(Integer id) {
        Servicio servicio = buscar(id);
        servicio.setDestacado(!Boolean.TRUE.equals(servicio.getDestacado()));
        return mapear(repository.saveAndFlush(servicio));
    }

    private Servicio buscar(Integer id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no encontrado"));
    }

    private Especialidad buscarEspecialidad(Integer id) {
        return especialidadRepository.findByIdAndActivoTrue(id)
            .orElseThrow(() -> new ResourceNotFoundException("Especialidad no disponible"));
    }

    private void validarNombre(Integer especialidadId, String nombre, Integer id) {
        boolean existe = id == null
            ? repository.existsByEspecialidadIdAndNombreIgnoreCase(especialidadId, nombre)
            : repository.existsByEspecialidadIdAndNombreIgnoreCaseAndIdNot(especialidadId, nombre, id);

        if (existe) {
            throw new ConflictException(
                "Ya existe un servicio con ese nombre en la especialidad seleccionada"
            );
        }
    }

    private void validarSedes(List<Integer> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new ConflictException("La selección de sedes no es válida");
        }

        Set<Integer> unicos = new HashSet<>(ids);

        if (unicos.size() != ids.size()) {
            throw new ConflictException("No puedes seleccionar una sede más de una vez");
        }

        if (sedeRepository.findAllById(unicos).size() != unicos.size()) {
            throw new ResourceNotFoundException("Una de las sedes seleccionadas no existe");
        }
    }

    private void actualizarDatos(Servicio servicio, ServicioRequest request) {
        servicio.setNombre(request.nombre().trim());
        servicio.setDescripcion(request.descripcion() == null || request.descripcion().isBlank()
            ? null : request.descripcion().trim());

        servicio.setDuracionMinutos(request.duracionMinutos().shortValue());
        servicio.setPrecioReferencial(request.precioReferencial());
        if (request.destacado() != null) {
            servicio.setDestacado(request.destacado());
        }
    }

    private void actualizarSedes(Integer servicioId, List<Integer> sedeIds) {
        Set<Integer> seleccionadas = new HashSet<>(sedeIds);

        List<ServicioSede> existentes =
            servicioSedeRepository.findByIdServicioOrderByIdSedeAsc(servicioId);

        Set<Integer> registradas = new HashSet<>();

        for (ServicioSede relacion : existentes) {
            relacion.setActivo(seleccionadas.contains(relacion.getIdSede()));
            registradas.add(relacion.getIdSede());
        }

        servicioSedeRepository.saveAll(existentes);

        for (Integer sedeId : seleccionadas) {
            if (registradas.contains(sedeId)) continue;

            ServicioSede relacion = new ServicioSede();
            relacion.setIdServicio(servicioId);
            relacion.setIdSede(sedeId);
            relacion.setActivo(true);

            servicioSedeRepository.save(relacion);
        }

        servicioSedeRepository.flush();
    }

    private ServicioResponse mapear(Servicio servicio) {
        List<Integer> sedeIds = servicioSedeRepository
            .findByIdServicioOrderByIdSedeAsc(servicio.getId())
            .stream()
            .filter(r -> Boolean.TRUE.equals(r.getActivo()))
            .map(ServicioSede::getIdSede)
            .toList();

        return new ServicioResponse(
            servicio.getId(),
            servicio.getEspecialidad().getId(),
            servicio.getEspecialidad().getNombre(),
            servicio.getNombre(),
            servicio.getDescripcion(),
            servicio.getDuracionMinutos().intValue(),
            servicio.getPrecioReferencial(),
            servicio.getActivo(),
            Boolean.TRUE.equals(servicio.getDestacado()),
            sedeIds
        );
    }
}