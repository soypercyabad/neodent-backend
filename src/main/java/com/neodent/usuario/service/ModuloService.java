package com.neodent.usuario.service;

import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.dto.request.ModuloRequest;
import com.neodent.usuario.dto.response.ModuloResponse;
import com.neodent.usuario.model.Modulo;
import com.neodent.usuario.repository.ModuloRepository;
import com.neodent.usuario.dto.response.ModuloResumenEstadosResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ModuloService {

    private final ModuloRepository moduloRepository;

    @Transactional(readOnly = true)
    public Page<ModuloResponse> listarPaginado(
        String buscar,
        Boolean activo,
        int page,
        int size,
        String sortBy,
        String direction
    ) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;

        String campoOrden = switch (sortBy != null ? sortBy.toLowerCase() : "orden") {
            case "nombre" -> "nombre";
            case "codigo" -> "codigo";
            case "ruta" -> "ruta";
            case "activo" -> "activo";
            default -> "orden";
        };

        Sort.Direction sortDirection = "desc".equalsIgnoreCase(direction)
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, campoOrden));

        return moduloRepository.buscarPaginado(buscarNormalizado, activo, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ModuloResumenEstadosResponse resumenEstados(String buscar) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;
        List<Object[]> resultados = moduloRepository.contarPorEstados(buscarNormalizado);

        if (resultados.isEmpty() || resultados.get(0) == null) {
            return new ModuloResumenEstadosResponse(0L, 0L, 0L);
        }

        Object[] fila = resultados.get(0);
        long todos = fila[0] instanceof Number n ? n.longValue() : 0L;
        long activos = fila[1] instanceof Number n ? n.longValue() : 0L;
        long inactivos = fila[2] instanceof Number n ? n.longValue() : 0L;

        return new ModuloResumenEstadosResponse(todos, activos, inactivos);
    }

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

        if (ruta != null && moduloRepository.existsByRutaIgnoreCase(ruta)) {
            throw new ConflictException("Ya existe un módulo con la ruta: " + ruta);
        }

        if (request.idModuloPadre() != null) {
            Modulo padre = moduloRepository.findById(request.idModuloPadre())
                .orElseThrow(() -> new ResourceNotFoundException("El módulo padre especificado no existe: " + request.idModuloPadre()));
            if (padre.getRuta() != null && !padre.getRuta().isBlank()) {
                throw new BusinessException("No se puede asignar como padre a un módulo que tiene ruta (" + padre.getNombre() + "). Solo los módulos contenedores sin ruta pueden tener submódulos.");
            }
        }

        Modulo modulo = new Modulo();
        modulo.setCodigo(codigo);
        modulo.setNombre(request.nombre().trim());
        modulo.setDescripcion(limpiar(request.descripcion()));
        modulo.setRuta(ruta);
        modulo.setIcono(request.icono().trim());
        modulo.setOrden(request.orden());
        modulo.setIdModuloPadre(request.idModuloPadre());
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

        if (ruta != null && moduloRepository.existsByRutaIgnoreCaseAndIdNot(ruta, id)) {
            throw new ConflictException("Ya existe otro módulo con la ruta: " + ruta);
        }

        if (request.idModuloPadre() != null) {
            if (request.idModuloPadre().equals(id)) {
                throw new BusinessException("Un módulo no puede ser padre de sí mismo");
            }
            Modulo padre = moduloRepository.findById(request.idModuloPadre())
                .orElseThrow(() -> new ResourceNotFoundException("El módulo padre especificado no existe: " + request.idModuloPadre()));
            if (padre.getRuta() != null && !padre.getRuta().isBlank()) {
                throw new BusinessException("No se puede asignar como padre a un módulo que tiene ruta (" + padre.getNombre() + "). Solo los módulos contenedores sin ruta pueden tener submódulos.");
            }
        }

        if (ruta != null && !ruta.isBlank()) {
            boolean tieneHijos = moduloRepository.existsByIdModuloPadre(id);
            if (tieneHijos) {
                throw new BusinessException("Este módulo contiene submódulos hijos. No puede tener una ruta de navegación; debe ser un contenedor agrupador.");
            }
        }

        modulo.setCodigo(codigo);
        modulo.setNombre(request.nombre().trim());
        modulo.setDescripcion(limpiar(request.descripcion()));
        modulo.setRuta(ruta);
        modulo.setIcono(request.icono().trim());
        modulo.setOrden(request.orden());
        modulo.setIdModuloPadre(request.idModuloPadre());

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
        if (ruta == null || ruta.isBlank()) {
            return null;
        }
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
        String nombrePadre = null;
        if (m.getIdModuloPadre() != null) {
            nombrePadre = moduloRepository.findById(m.getIdModuloPadre())
                .map(Modulo::getNombre)
                .orElse(null);
        }

        return new ModuloResponse(
            m.getId(),
            m.getCodigo(),
            m.getNombre(),
            m.getDescripcion(),
            m.getRuta(),
            m.getIcono(),
            m.getOrden(),
            m.getIdModuloPadre(),
            nombrePadre,
            m.getActivo()
        );
    }
}
