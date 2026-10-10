package com.neodent.usuario.service;

import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.dto.request.ModuloRequest;
import com.neodent.usuario.dto.response.ModuloResponse;
import com.neodent.usuario.model.Modulo;
import com.neodent.usuario.repository.ModuloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuloService {

    private final ModuloRepository moduloRepository;

    @Transactional(readOnly = true)
    public List<ModuloResponse> listar(Boolean activo) {
        List<Modulo> modulos = activo == null
            ? moduloRepository.findAllByOrderByOrdenAsc()
            : activo
                ? moduloRepository.findAllByActivoTrueOrderByOrdenAsc()
                : moduloRepository.findAllByOrderByOrdenAsc()
                    .stream()
                    .filter(m -> !Boolean.TRUE.equals(m.getActivo()))
                    .toList();

        return modulos.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ModuloResponse obtener(Integer id) {
        return toResponse(obtenerOFail(id));
    }

    @Transactional
    public ModuloResponse crear(ModuloRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        String ruta = normalizarRuta(request.ruta());

        if (moduloRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un módulo con el código: " + codigo);
        }

        if (moduloRepository.existsByRutaIgnoreCase(ruta)) {
            throw new ConflictException("Ya existe un módulo con la ruta: " + ruta);
        }

        Modulo modulo = new Modulo();
        modulo.setCodigo(codigo);
        modulo.setNombre(request.nombre().trim());
        modulo.setDescripcion(limpiar(request.descripcion()));
        modulo.setRuta(ruta);
        modulo.setIcono(request.icono().trim());
        modulo.setOrden(request.orden());
        modulo.setActivo(true);

        return toResponse(moduloRepository.save(modulo));
    }

    @Transactional
    public ModuloResponse actualizar(Integer id, ModuloRequest request) {
        Modulo modulo = obtenerOFail(id);

        String codigo = normalizarCodigo(request.codigo());
        String ruta = normalizarRuta(request.ruta());

        if (moduloRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, id)) {
            throw new ConflictException("Ya existe otro módulo con el código: " + codigo);
        }

        if (moduloRepository.existsByRutaIgnoreCaseAndIdNot(ruta, id)) {
            throw new ConflictException("Ya existe otro módulo con la ruta: " + ruta);
        }

        modulo.setCodigo(codigo);
        modulo.setNombre(request.nombre().trim());
        modulo.setDescripcion(limpiar(request.descripcion()));
        modulo.setRuta(ruta);
        modulo.setIcono(request.icono().trim());
        modulo.setOrden(request.orden());

        return toResponse(moduloRepository.save(modulo));
    }

    @Transactional
    public ModuloResponse activar(Integer id) {
        Modulo modulo = obtenerOFail(id);

        if (Boolean.TRUE.equals(modulo.getActivo())) {
            throw new ConflictException("El módulo ya se encuentra activo");
        }

        modulo.setActivo(true);
        return toResponse(moduloRepository.save(modulo));
    }

    @Transactional
    public ModuloResponse desactivar(Integer id) {
        Modulo modulo = obtenerOFail(id);

        if (Boolean.FALSE.equals(modulo.getActivo())) {
            throw new ConflictException("El módulo ya se encuentra inactivo");
        }

        modulo.setActivo(false);
        return toResponse(moduloRepository.save(modulo));
    }

    private Modulo obtenerOFail(Integer id) {
        return moduloRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado: " + id));
    }

    private String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase().replace(" ", "_");
    }

    private String normalizarRuta(String ruta) {
        String r = ruta.trim().toLowerCase();
        if (!r.startsWith("/")) {
            r = "/" + r;
        }
        return r;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private ModuloResponse toResponse(Modulo m) {
        return new ModuloResponse(
            m.getId(),
            m.getCodigo(),
            m.getNombre(),
            m.getDescripcion(),
            m.getRuta(),
            m.getIcono(),
            m.getOrden(),
            m.getActivo()
        );
    }
}
