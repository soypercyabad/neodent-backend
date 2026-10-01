package com.neodent.cita.service;

import com.neodent.cita.dto.request.TipoBloqueoHorarioRequest;
import com.neodent.cita.dto.response.TipoBloqueoHorarioResponse;
import com.neodent.cita.model.TipoBloqueoHorario;
import com.neodent.cita.repository.TipoBloqueoHorarioRepository;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TipoBloqueoHorarioService {

    private final TipoBloqueoHorarioRepository repository;

    @Transactional(readOnly = true)
    public List<TipoBloqueoHorarioResponse> listarActivos() {
        return repository
            .findByActivoTrueOrderByNombreAsc()
            .stream()
            .map(this::mapear)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<TipoBloqueoHorarioResponse> listarTodos() {
        return repository
            .findAllByOrderByNombreAsc()
            .stream()
            .map(this::mapear)
            .toList();
    }

    @Transactional(readOnly = true)
    public TipoBloqueoHorarioResponse obtener(Integer id) {
        return mapear(buscar(id));
    }

    @Transactional
    public TipoBloqueoHorarioResponse crear(TipoBloqueoHorarioRequest request) {
        validarReglas(request);

        String codigo = normalizarCodigo(request.codigo());
        String nombre = request.nombre().trim();

        if (repository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un tipo de bloqueo con ese código");
        }

        if (repository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe un tipo de bloqueo con ese nombre");
        }

        TipoBloqueoHorario tipo = new TipoBloqueoHorario();
        aplicarDatos(tipo, request);
        tipo.setActivo(true);
        return guardar(tipo);
    }

    @Transactional
    public TipoBloqueoHorarioResponse actualizar(Integer id, TipoBloqueoHorarioRequest request) {
        validarReglas(request);
        TipoBloqueoHorario tipo = buscar(id);
        String codigo = normalizarCodigo(request.codigo());
        String nombre = request.nombre().trim();
        if (repository.existsByCodigoIgnoreCaseAndIdNot(codigo,id)) {
            throw new ConflictException("Ya existe otro tipo de bloqueo con ese código");
        }
        if (repository.existsByNombreIgnoreCaseAndIdNot(nombre,id)) {
            throw new ConflictException("Ya existe otro tipo de bloqueo con ese nombre");
        }
        aplicarDatos(tipo, request);
        return guardar(tipo);
    }

    @Transactional
    public TipoBloqueoHorarioResponse cambiarEstado(Integer id, Boolean activo) {
        TipoBloqueoHorario tipo = buscar(id);
        if (tipo.getActivo().equals(activo)) return mapear(tipo);
        tipo.setActivo(activo);
        return guardar(tipo);
    }

    private TipoBloqueoHorario buscar(Integer id) {
        return repository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Tipo de bloqueo no encontrado"));
    }

    private void aplicarDatos(TipoBloqueoHorario tipo, TipoBloqueoHorarioRequest request) {
        tipo.setCodigo(normalizarCodigo(request.codigo()));
        tipo.setNombre(request.nombre().trim());
        tipo.setDescripcion(normalizarTexto(request.descripcion()));
        tipo.setRequiereOdontologo(request.requiereOdontologo());
        tipo.setRequiereSede(request.requiereSede());
        tipo.setPermiteOdontologo(request.permiteOdontologo());
        tipo.setPermiteSede(request.permiteSede());
    }

    private void validarReglas(TipoBloqueoHorarioRequest request) {
        if (request.requiereOdontologo()&& !request.permiteOdontologo()) {
            throw new ConflictException("Un tipo que requiere odontólogo también debe permitir odontólogo");
        }
        if (request.requiereSede() && !request.permiteSede()) {
            throw new ConflictException("Un tipo que requiere sede también debe permitir sede");
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo
            .trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    private String normalizarTexto(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return valor.trim();
    }

    private TipoBloqueoHorarioResponse guardar(TipoBloqueoHorario tipo) {
        try {
            return mapear(repository.saveAndFlush(tipo));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("No se pudo guardar el tipo de bloqueo. Verifica que el código y el nombre no estén duplicados");
        }
    }

    private TipoBloqueoHorarioResponse mapear(TipoBloqueoHorario tipo) {
        return new TipoBloqueoHorarioResponse(
            tipo.getId(),
            tipo.getCodigo(),
            tipo.getNombre(),
            tipo.getDescripcion(),
            tipo.getRequiereOdontologo(),
            tipo.getRequiereSede(),
            tipo.getPermiteOdontologo(),
            tipo.getPermiteSede(),
            tipo.getActivo(),
            tipo.getFechaCreacion(),
            tipo.getFechaActualizacion()
        );
    }
}