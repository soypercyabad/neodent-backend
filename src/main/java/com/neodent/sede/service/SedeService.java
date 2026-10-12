package com.neodent.sede.service;

import com.neodent.sede.dto.response.SedeResumenEstadosResponse;
import com.neodent.sede.dto.request.SedeRequest;
import com.neodent.sede.dto.response.SedeCatalogoResponse;
import com.neodent.sede.dto.response.SedeResponse;
import com.neodent.sede.model.Sede;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeService {

    private final SedeRepository repository;

    @Transactional(readOnly = true)
    public Page<SedeResponse> listarPaginado(
        String buscar,
        Boolean activo,
        int page,
        int size,
        String sortBy,
        String direction
    ) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;

        String campoOrden = switch (sortBy != null ? sortBy.toLowerCase() : "nombre") {
            case "direccion" -> "direccion";
            case "distrito" -> "distrito";
            case "departamento" -> "departamento";
            case "activo" -> "activo";
            case "fechacreacion" -> "fechaCreacion";
            default -> "nombre";
        };

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, campoOrden));

        return repository.buscarPaginado(buscarNormalizado, activo, pageable)
            .map(this::mapear);
    }

    @Transactional(readOnly = true)
    public SedeResumenEstadosResponse resumenEstados(String buscar) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;
        List<Object[]> resultados = repository.contarPorEstados(buscarNormalizado);

        if (resultados.isEmpty() || resultados.get(0) == null) {
            return new SedeResumenEstadosResponse(0L, 0L, 0L);
        }

        Object[] fila = resultados.get(0);
        long todas = fila[0] instanceof Number n ? n.longValue() : 0L;
        long activas = fila[1] instanceof Number n ? n.longValue() : 0L;
        long inactivas = fila[2] instanceof Number n ? n.longValue() : 0L;

        return new SedeResumenEstadosResponse(todas, activas, inactivas);
    }

    @Transactional(readOnly = true)
    public List<SedeCatalogoResponse> catalogo() {
        return repository.findByActivoTrueOrderByNombreAsc()
            .stream()
            .map(s -> new SedeCatalogoResponse(s.getId(), s.getNombre(), s.getDistrito()))
            .toList();
    }

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