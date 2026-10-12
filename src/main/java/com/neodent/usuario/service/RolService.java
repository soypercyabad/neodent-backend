package com.neodent.usuario.service;

import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.dto.request.RolRequest;
import com.neodent.usuario.dto.response.RolCatalogoResponse;
import com.neodent.usuario.dto.response.RolModuloResponse;
import com.neodent.usuario.dto.response.RolResponse;
import com.neodent.usuario.model.Modulo;
import com.neodent.usuario.model.Rol;
import com.neodent.usuario.repository.ModuloRepository;
import com.neodent.usuario.repository.RolRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import com.neodent.usuario.dto.response.RolResumenEstadosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RolService {
    private static final Set<String> ROLES_SISTEMA = Set.of(
        AppConstants.Roles.ADMIN,
        AppConstants.Roles.RECEPCIONISTA,
        AppConstants.Roles.ODONTOLOGO,
        AppConstants.Roles.PACIENTE
    );

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ModuloRepository moduloRepository;

    @Transactional(readOnly = true)
    public Page<RolResponse> listarPaginado(
        String buscar,
        String tipo,
        Boolean activo,
        int page,
        int size,
        String sortBy,
        String direction
    ) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;
        String tipoNormalizado = tipo != null && !tipo.isBlank() ? tipo.trim().toUpperCase() : null;

        String campoOrden = switch (sortBy != null ? sortBy.toLowerCase() : "nombre") {
            case "id" -> "id";
            case "descripcion" -> "descripcion";
            case "activo" -> "activo";
            default -> "nombre";
        };

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, campoOrden));

        return rolRepository.buscarPaginado(buscarNormalizado, tipoNormalizado, activo, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public RolResumenEstadosResponse resumenEstados(String buscar) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;
        List<Object[]> resultados = rolRepository.contarPorEstados(buscarNormalizado);

        if (resultados.isEmpty() || resultados.get(0) == null) {
            return new RolResumenEstadosResponse(0L, 0L, 0L, 0L, 0L);
        }

        Object[] fila = resultados.get(0);
        long todos = fila[0] instanceof Number n ? n.longValue() : 0L;
        long activos = fila[1] instanceof Number n ? n.longValue() : 0L;
        long inactivos = fila[2] instanceof Number n ? n.longValue() : 0L;
        long sistema = fila[3] instanceof Number n ? n.longValue() : 0L;
        long personalizados = fila[4] instanceof Number n ? n.longValue() : 0L;

        return new RolResumenEstadosResponse(todos, activos, inactivos, sistema, personalizados);
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listar(Boolean activo) {
        List<Rol> roles = activo == null
            ? rolRepository.findAllByOrderByNombreAsc()
            : activo
                ? rolRepository.findAllByActivoTrueOrderByNombreAsc()
                : rolRepository.findAllByOrderByNombreAsc()
                    .stream()
                    .filter(r -> !Boolean.TRUE.equals(r.getActivo()))
                    .toList();

        return roles.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RolResponse obtener(Integer id) {
        return toResponse(obtenerOFail(id));
    }

    @Transactional(readOnly = true)
    public List<RolCatalogoResponse> catalogo() {
        return rolRepository.findAllByActivoTrueOrderByNombreAsc()
            .stream()
            .map(r -> new RolCatalogoResponse(r.getId(), r.getNombre()))
            .toList();
    }

    @Transactional
    public RolResponse crear(RolRequest request) {
        String nombre = normalizarNombre(request.nombre());

        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe un rol con ese nombre");
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(limpiar(request.descripcion()));
        rol.setActivo(true);

        if (request.modulosIds() != null && !request.modulosIds().isEmpty()) {
            List<Modulo> modulos = moduloRepository.findAllByIdInAndActivoTrue(request.modulosIds());
            rol.setModulos(new HashSet<>(modulos));
        }

        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse actualizar(Integer id, RolRequest request) {
        Rol rol = obtenerOFail(id);
        String actual = rol.getNombre().toUpperCase();
        String nuevo = normalizarNombre(request.nombre());

        if (ROLES_SISTEMA.contains(actual) && !actual.equals(nuevo)) {
            throw new ConflictException("Los roles del sistema no pueden cambiar de nombre");
        }

        if (rolRepository.existsByNombreIgnoreCaseAndIdNot(nuevo, id)) {
            throw new ConflictException("Ya existe un rol con ese nombre");
        }

        rol.setNombre(nuevo);
        rol.setDescripcion(limpiar(request.descripcion()));

        if (request.modulosIds() != null) {
            List<Modulo> modulos = moduloRepository.findAllByIdInAndActivoTrue(request.modulosIds());
            rol.setModulos(new HashSet<>(modulos));
        }

        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse activar(Integer id) {
        Rol rol = obtenerOFail(id);

        if (Boolean.TRUE.equals(rol.getActivo())) {
            throw new ConflictException("El rol ya se encuentra activo");
        }

        rol.setActivo(true);
        return toResponse(rolRepository.save(rol));
    }

    @Transactional
    public RolResponse desactivar(Integer id) {
        Rol rol = obtenerOFail(id);

        if (ROLES_SISTEMA.contains(rol.getNombre().toUpperCase())) {
            throw new ConflictException("Los roles del sistema no pueden desactivarse");
        }

        if (!Boolean.TRUE.equals(rol.getActivo())) {
            throw new ConflictException("El rol ya se encuentra inactivo");
        }

        if (usuarioRepository.contarUsuariosConRol(id) > 0) {
            throw new ConflictException("No se puede desactivar un rol que está asignado a usuarios");
        }

        rol.setActivo(false);
        return toResponse(rolRepository.save(rol));
    }

    private Rol obtenerOFail(Integer id) {
        return rolRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));
    }

    private String normalizarNombre(String valor) {
            String nombre = valor.trim().toUpperCase()
            .replaceAll("[^A-Z0-9_]+", "_")
            .replaceAll("^_+|_+$", "");

        if (nombre.isBlank()) {
            throw new ConflictException("El nombre del rol no es válido");
        }

        return nombre;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private RolResponse toResponse(Rol rol) {
        List<RolModuloResponse> modulos = rol.getModulos() == null ? List.of() :
            rol.getModulos().stream()
                .filter(m -> Boolean.TRUE.equals(m.getActivo()))
                .sorted(Comparator.comparingInt(Modulo::getOrden))
                .map(m -> new RolModuloResponse(
                    m.getId(),
                    m.getCodigo(),
                    m.getNombre()
                ))
                .toList();

        return new RolResponse(
            rol.getId(),
            rol.getNombre(),
            rol.getDescripcion(),
            rol.getActivo(),
            ROLES_SISTEMA.contains(rol.getNombre().toUpperCase()),
            modulos
        );
    }
}