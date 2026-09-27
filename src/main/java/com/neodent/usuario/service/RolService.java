package com.neodent.usuario.service;

import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.dto.request.RolRequest;
import com.neodent.usuario.dto.response.RolResponse;
import com.neodent.usuario.model.Rol;
import com.neodent.usuario.repository.RolRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        return new RolResponse(
            rol.getId(),
            rol.getNombre(),
            rol.getDescripcion(),
            rol.getActivo(),
            ROLES_SISTEMA.contains(rol.getNombre().toUpperCase())
        );
    }
}