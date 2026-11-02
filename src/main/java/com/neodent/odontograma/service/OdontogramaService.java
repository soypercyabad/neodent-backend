package com.neodent.odontograma.service;

import com.neodent.cita.repository.CitaRepository;
import com.neodent.historia.model.HistoriaClinica;
import com.neodent.historia.repository.HistoriaClinicaRepository;
import com.neodent.historia.service.HistoriaClinicaService;
import com.neodent.odontograma.dto.*;
import com.neodent.odontograma.model.CondicionOdontograma;
import com.neodent.odontograma.model.CatalogoPiezaDental;
import com.neodent.odontograma.model.Odontograma;
import com.neodent.odontograma.model.OdontogramaDetalle;
import com.neodent.odontograma.repository.CondicionOdontogramaRepository;
import com.neodent.odontograma.repository.CatalogoPiezaDentalRepository;
import com.neodent.odontograma.repository.OdontogramaDetalleRepository;
import com.neodent.odontograma.repository.OdontogramaRepository;
import com.neodent.odontologo.model.Odontologo;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.personal.model.Personal;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdontogramaService {

    private final OdontogramaRepository odontogramaRepository;
    private final OdontogramaDetalleRepository detalleRepository;
    private final CondicionOdontogramaRepository condicionRepository;
    private final CatalogoPiezaDentalRepository catalogoPiezaRepository;
    private final HistoriaClinicaRepository historiaRepository;
    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaService historiaClinicaService;
    private final CitaRepository citaRepository;

    private static final List<CondicionOdontograma> CONDICIONES_CATALOGO_COMPLETO = List.of(
        new CondicionOdontograma("CARIES", "Caries Dental", "Lesión cariosa activa o recidivante", "#EF4444", true, "SUPERFICIE"),
        new CondicionOdontograma("RESTAURACION", "Restauración / Obturación", "Resina compuesta, amalgama o ionómero", "#3B82F6", true, "SUPERFICIE"),
        new CondicionOdontograma("CORONA", "Corona Protésica", "Corona fija o funda dental protésica", "#A855F7", true, "PIEZA_COMPLETA"),
        new CondicionOdontograma("AUSENTE", "Pieza Ausente", "Pieza dental extraída o ausente", "#64748B", true, "PIEZA_COMPLETA"),
        new CondicionOdontograma("ENDODONCIA", "Endodoncia / Conducto", "Tratamiento pulpar o conducto realizado", "#F97316", true, "PIEZA_COMPLETA"),
        new CondicionOdontograma("SELLANTE", "Sellante de Fosas", "Sellado preventivo de fosas y fisuras", "#06B6D4", true, "SUPERFICIE"),
        new CondicionOdontograma("FRACTURA", "Fractura / Fisura", "Pérdida de tejido dentario por traumatismo o fisura", "#E11D48", true, "AMBOS"),
        new CondicionOdontograma("IMPLANTE", "Implante Dental", "Implante osteointegrado o prótesis sobre implante", "#8B5CF6", true, "PIEZA_COMPLETA"),
        new CondicionOdontograma("PROTESIS_REMOVIBLE", "Prótesis Removible", "Aparato protésico removible parcial o total", "#F59E0B", true, "PIEZA_COMPLETA"),
        new CondicionOdontograma("DIENTE_IMPACTADO", "Diente Impactado / Retenido", "Pieza dental no erupcionada retenida en hueso", "#EC4899", true, "PIEZA_COMPLETA"),
        new CondicionOdontograma("SANO", "Sano / Normal", "Pieza íntegra sin hallazgos patológicos", "#10B981", true, "AMBOS")
    );

    @Transactional
    public List<CondicionOdontogramaResponse> listarCondiciones() {
        // Sembrar condiciones clínicas iniciales solo si la tabla está completamente vacía
        if (condicionRepository.count() == 0) {
            for (CondicionOdontograma def : CONDICIONES_CATALOGO_COMPLETO) {
                try {
                    condicionRepository.save(new CondicionOdontograma(
                        def.getCodigo(), def.getNombre(), def.getDescripcion(), def.getColorHexadecimal(), true, def.getAplicaA()
                    ));
                } catch (Exception ex) {
                    log.debug("Condición {} no pudo sembrarse: {}", def.getCodigo(), ex.getMessage());
                }
            }
        }

        List<CondicionOdontograma> condiciones = condicionRepository.findByActivoTrueOrderByIdAsc();

        return condiciones.stream()
            .map(c -> new CondicionOdontogramaResponse(
                c.getId(),
                c.getCodigo(),
                c.getNombre(),
                c.getDescripcion(),
                c.getColorHexadecimal(),
                c.getActivo(),
                c.getAplicaA() != null ? c.getAplicaA() : "AMBOS"
            ))
            .toList();
    }

    @Transactional
    public CondicionOdontogramaResponse crearCondicion(CrearCondicionRequest request) {
        String codigo = request.codigo() != null && !request.codigo().isBlank()
            ? request.codigo().toUpperCase().trim().replaceAll("[^A-Z0-9_]", "_")
            : request.nombre().toUpperCase().trim().replaceAll("[^A-Z0-9_]", "_");

        if (codigo.length() > 30) {
            codigo = codigo.substring(0, 30);
        }

        if (condicionRepository.findByCodigoIgnoreCase(codigo).isPresent()) {
            codigo = codigo + "_" + (System.currentTimeMillis() % 1000);
            if (codigo.length() > 30) {
                codigo = codigo.substring(0, 30);
            }
        }

        String color = request.colorHexadecimal() != null && request.colorHexadecimal().startsWith("#")
            ? request.colorHexadecimal().trim()
            : "#3B82F6";

        String aplicaA = "AMBOS";
        if (request.aplicaA() != null && !request.aplicaA().isBlank()) {
            String candidate = request.aplicaA().trim().toUpperCase();
            if ("PIEZA_COMPLETA".equals(candidate) || "SUPERFICIE".equals(candidate)) {
                aplicaA = candidate;
            }
        }

        CondicionOdontograma nueva = new CondicionOdontograma(
            codigo,
            request.nombre().trim(),
            request.descripcion() != null && !request.descripcion().isBlank() ? request.descripcion().trim() : null,
            color,
            true,
            aplicaA
        );

        CondicionOdontograma guardada = condicionRepository.save(nueva);
        log.info("Nueva condición dental creada por usuario: ID={}, Código={}, Nombre={}, AplicaA={}",
            guardada.getId(), guardada.getCodigo(), guardada.getNombre(), guardada.getAplicaA());

        return new CondicionOdontogramaResponse(
            guardada.getId(),
            guardada.getCodigo(),
            guardada.getNombre(),
            guardada.getDescripcion(),
            guardada.getColorHexadecimal(),
            guardada.getActivo(),
            guardada.getAplicaA()
        );
    }

    @Transactional
    public CondicionOdontogramaResponse actualizarCondicion(Integer id, CrearCondicionRequest request) {
        CondicionOdontograma c = condicionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Condición odontológica no encontrada"));

        if (request.nombre() != null && !request.nombre().isBlank()) {
            c.setNombre(request.nombre().trim());
        }
        if (request.descripcion() != null) {
            c.setDescripcion(request.descripcion().isBlank() ? null : request.descripcion().trim());
        }
        if (request.colorHexadecimal() != null && request.colorHexadecimal().startsWith("#")) {
            c.setColorHexadecimal(request.colorHexadecimal().trim());
        }
        if (request.aplicaA() != null && !request.aplicaA().isBlank()) {
            String candidate = request.aplicaA().trim().toUpperCase();
            if ("PIEZA_COMPLETA".equals(candidate) || "SUPERFICIE".equals(candidate) || "AMBOS".equals(candidate)) {
                c.setAplicaA(candidate);
            }
        }

        CondicionOdontograma guardada = condicionRepository.save(c);
        log.info("Condición dental actualizada en BD: ID={}, Nombre={}, AplicaA={}",
            guardada.getId(), guardada.getNombre(), guardada.getAplicaA());

        return new CondicionOdontogramaResponse(
            guardada.getId(),
            guardada.getCodigo(),
            guardada.getNombre(),
            guardada.getDescripcion(),
            guardada.getColorHexadecimal(),
            guardada.getActivo(),
            guardada.getAplicaA()
        );
    }

    @Transactional
    public void eliminarCondicion(Integer id) {
        CondicionOdontograma c = condicionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Condición odontológica no encontrada"));

        boolean enUso = detalleRepository.existsByCondicionId(id);
        if (enUso) {
            c.setActivo(false);
            condicionRepository.save(c);
            log.info("Condición dental desactivada en BD: ID={}, Código={}", id, c.getCodigo());
        } else {
            condicionRepository.delete(c);
            log.info("Condición dental eliminada permanentemente de la BD: ID={}, Código={}", id, c.getCodigo());
        }
    }

    @Transactional(readOnly = true)
    public List<PiezaDentalCatalogoResponse> listarCatalogoPiezas() {
        return catalogoPiezaRepository.findByActivoTrueOrderByNumeroPiezaAsc().stream()
            .map(p -> new PiezaDentalCatalogoResponse(
                p.getNumeroPieza(),
                p.getNombre(),
                p.getTipoDenticion(),
                p.getCuadrante(),
                p.getActivo()
            ))
            .toList();
    }

    @Transactional
    public PiezaDentalCatalogoResponse actualizarNombrePieza(Integer numeroPieza, ActualizarNombrePiezaRequest request) {
        CatalogoPiezaDental pieza = catalogoPiezaRepository.findById(numeroPieza)
            .orElseThrow(() -> new ResourceNotFoundException("Pieza dental no encontrada en el catálogo: " + numeroPieza));

        pieza.setNombre(request.nombre().trim());
        CatalogoPiezaDental guardada = catalogoPiezaRepository.save(pieza);
        log.info("Nombre de pieza dental {} actualizado en catálogo a: '{}'", numeroPieza, guardada.getNombre());

        return new PiezaDentalCatalogoResponse(
            guardada.getNumeroPieza(),
            guardada.getNombre(),
            guardada.getTipoDenticion(),
            guardada.getCuadrante(),
            guardada.getActivo()
        );
    }

    @Transactional
    public OdontogramaResponse obtenerOdontogramaActual(Long pacienteId, Long usuarioId) {
        HistoriaClinica historia = obtenerOCrearHistoria(pacienteId);

        Odontograma odontograma = odontogramaRepository.findFirstByHistoriaClinicaIdOrderByFechaDesc(historia.getId())
            .orElseGet(() -> crearOdontogramaInicial(historia, usuarioId));

        return mapearOdontograma(odontograma);
    }

    @Transactional
    public OdontogramaResponse actualizarPieza(Long pacienteId, ActualizarPiezaRequest request, Long usuarioId) {
        HistoriaClinica historia = obtenerOCrearHistoria(pacienteId);

        Odontograma odontograma = odontogramaRepository.findFirstByHistoriaClinicaIdOrderByFechaDesc(historia.getId())
            .orElseGet(() -> crearOdontogramaInicial(historia, usuarioId));

        // Resolución tolerante y robusta de la condición
        CondicionOdontograma condicion = null;
        if (request.idCondicion() != null) {
            condicion = condicionRepository.findById(request.idCondicion()).orElse(null);
        }
        if (condicion == null && request.codigoCondicion() != null && !request.codigoCondicion().isBlank()) {
            condicion = condicionRepository.findByCodigoIgnoreCase(request.codigoCondicion().trim()).orElse(null);
        }
        // Fallback por índice si vino un ID secuencial del catálogo por defecto (ej. ID 7 = Fractura)
        if (condicion == null && request.idCondicion() != null && request.idCondicion() > 0 && request.idCondicion() <= CONDICIONES_CATALOGO_COMPLETO.size()) {
            CondicionOdontograma def = CONDICIONES_CATALOGO_COMPLETO.get(request.idCondicion() - 1);
            condicion = condicionRepository.findByCodigoIgnoreCase(def.getCodigo()).orElseGet(() ->
                condicionRepository.save(new CondicionOdontograma(def.getCodigo(), def.getNombre(), def.getDescripcion(), def.getColorHexadecimal(), true, def.getAplicaA()))
            );
        }
        if (condicion == null) {
            throw new ResourceNotFoundException("Condición dental no encontrada con ID: " + request.idCondicion());
        }

        String sup = normalizarSuperficie(request.superficie());
        Byte piezaByte = request.numeroPieza() != null ? request.numeroPieza().byteValue() : null;
        String ambito = condicion.getAplicaA() != null ? condicion.getAplicaA().toUpperCase() : "AMBOS";

        // Normalización automática de ámbito para proteger transiciones de estado y compatibilidad clínica
        if ("PIEZA_COMPLETA".equals(ambito)) {
            sup = "GENERAL";
        } else if ("SUPERFICIE".equals(ambito) && (sup == null || "GENERAL".equalsIgnoreCase(sup))) {
            sup = "OCLUSAL";
        }
        boolean esPiezaCompleta = sup == null || "GENERAL".equalsIgnoreCase(sup);

        // Si la condición es "SANO", removemos cualquier hallazgo patológico previo en esa superficie/pieza
        if ("SANO".equalsIgnoreCase(condicion.getCodigo())) {
            if (esPiezaCompleta) {
                detalleRepository.deleteByOdontogramaIdAndNumeroPieza(odontograma.getId(), piezaByte);
            } else {
                detalleRepository.deleteByOdontogramaIdAndNumeroPiezaAndSuperficie(odontograma.getId(), piezaByte, sup);
            }
        } else {
            // Si es condición de pieza completa, limpia todas las superficies previas
            if (esPiezaCompleta) {
                detalleRepository.deleteByOdontogramaIdAndNumeroPieza(odontograma.getId(), piezaByte);
                sup = "GENERAL";
            } else {
                // Si es por superficie, elimina hallazgo previo de pieza completa y el previo en la misma superficie
                detalleRepository.deleteGeneralByOdontogramaIdAndNumeroPieza(odontograma.getId(), piezaByte);
                detalleRepository.deleteByOdontogramaIdAndNumeroPiezaAndSuperficie(odontograma.getId(), piezaByte, sup);
            }

            // Persistencia del estado (HALLAZGO, PRESUPUESTADO, PROGRAMADO, REALIZADO)
            String estado = request.estado() != null && !request.estado().isBlank()
                ? request.estado().toUpperCase().trim()
                : "HALLAZGO";

            String rawObs = request.observacion() != null ? request.observacion().trim() : "";
            String finalObs = "[ESTADO:" + estado + "]" + (rawObs.isBlank() ? "" : " " + rawObs);
            if (finalObs.length() > 255) {
                finalObs = finalObs.substring(0, 255);
            }

            OdontogramaDetalle detalle = new OdontogramaDetalle(
                odontograma,
                piezaByte,
                sup,
                condicion,
                finalObs
            );
            detalleRepository.save(detalle);
        }

        log.info("Pieza dental {} (superficie {}) actualizada con condición {} en odontograma ID: {}",
            request.numeroPieza(), sup, condicion.getNombre(), odontograma.getId());

        return mapearOdontograma(odontograma);
    }

    @Transactional
    public OdontogramaResponse limpiarPieza(Long pacienteId, Integer numeroPieza, String superficie) {
        HistoriaClinica historia = obtenerOCrearHistoria(pacienteId);

        Odontograma odontograma = odontogramaRepository.findFirstByHistoriaClinicaIdOrderByFechaDesc(historia.getId())
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró odontograma activo para el paciente."));

        Byte piezaByte = numeroPieza != null ? numeroPieza.byteValue() : null;
        String sup = normalizarSuperficie(superficie);
        if (sup == null || "GENERAL".equalsIgnoreCase(sup)) {
            detalleRepository.deleteByOdontogramaIdAndNumeroPieza(odontograma.getId(), piezaByte);
        } else {
            detalleRepository.deleteByOdontogramaIdAndNumeroPiezaAndSuperficie(odontograma.getId(), piezaByte, sup);
        }

        log.info("Hallazgo en pieza dental {} (superficie {}) eliminado.", numeroPieza, sup);
        return mapearOdontograma(odontograma);
    }

    private HistoriaClinica obtenerOCrearHistoria(Long pacienteId) {
        return historiaRepository.findByPacienteId(pacienteId)
            .orElseGet(() -> {
                Paciente paciente = pacienteRepository.findById(pacienteId)
                    .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + pacienteId));
                HistoriaClinica hc = new HistoriaClinica(paciente, null, null, null);
                return historiaRepository.save(hc);
            });
    }

    private Odontograma crearOdontogramaInicial(HistoriaClinica historia, Long usuarioId) {
        Odontologo odontologo = historiaClinicaService.resolverOdontologoPorUsuario(usuarioId);
        Odontograma odontograma = new Odontograma(historia, null, odontologo, "Odontograma inicial de apertura");
        return odontogramaRepository.save(odontograma);
    }

    private String normalizarSuperficie(String sup) {
        if (sup == null || sup.isBlank()) return null;
        String s = sup.trim().toUpperCase();
        return switch (s) {
            case "VESTIBULAR", "V" -> "VESTIBULAR";
            case "LINGUAL", "L", "PALATINO", "P", "PALATINA" -> "LINGUAL";
            case "MESIAL", "M" -> "MESIAL";
            case "DISTAL", "D" -> "DISTAL";
            case "OCLUSAL", "INCISAL", "O", "I" -> "OCLUSAL";
            default -> "GENERAL";
        };
    }

    private OdontogramaResponse mapearOdontograma(Odontograma o) {
        List<OdontogramaDetalle> detalles = detalleRepository.findByOdontogramaIdWithCondicion(o.getId());

        List<PiezaDentalDetalleDto> piezas = detalles.stream()
            .map(d -> {
                String rawObs = d.getObservacion();
                String estado = "HALLAZGO";
                String cleanObs = rawObs;
                if (rawObs != null && rawObs.startsWith("[ESTADO:")) {
                    int endIdx = rawObs.indexOf(']');
                    if (endIdx > 8) {
                        estado = rawObs.substring(8, endIdx).trim();
                        cleanObs = rawObs.substring(endIdx + 1).trim();
                    }
                }

                return new PiezaDentalDetalleDto(
                    d.getId(),
                    d.getNumeroPieza() != null ? d.getNumeroPieza().intValue() : null,
                    d.getSuperficie(),
                    d.getCondicion().getId(),
                    d.getCondicion().getCodigo(),
                    d.getCondicion().getNombre(),
                    d.getCondicion().getColorHexadecimal(),
                    cleanObs != null && !cleanObs.isBlank() ? cleanObs : null,
                    estado,
                    d.getFechaCreacion() != null ? d.getFechaCreacion() : o.getFecha()
                );
            })
            .toList();

        String odNombre = "Odontólogo General";
        String colegiatura = "-";
        if (o.getOdontologo() != null) {
            colegiatura = o.getOdontologo().getNumeroColegiatura();
            if (o.getOdontologo().getPersonal() != null) {
                Personal p = o.getOdontologo().getPersonal();
                odNombre = String.format("Dr(a). %s %s", p.getNombres(), p.getApellidoPaterno());
            }
        }

        return new OdontogramaResponse(
            o.getId(),
            o.getHistoriaClinica().getId(),
            o.getHistoriaClinica().getPaciente().getId(),
            o.getFecha(),
            o.getObservaciones(),
            o.getOdontologo() != null ? o.getOdontologo().getId() : null,
            odNombre,
            colegiatura,
            piezas
        );
    }
}
