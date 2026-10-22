package com.neodent.odontograma.service;

import com.neodent.cita.repository.CitaRepository;
import com.neodent.historia.model.HistoriaClinica;
import com.neodent.historia.repository.HistoriaClinicaRepository;
import com.neodent.historia.service.HistoriaClinicaService;
import com.neodent.odontograma.dto.*;
import com.neodent.odontograma.model.CondicionOdontograma;
import com.neodent.odontograma.model.Odontograma;
import com.neodent.odontograma.model.OdontogramaDetalle;
import com.neodent.odontograma.repository.CondicionOdontogramaRepository;
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
    private final HistoriaClinicaRepository historiaRepository;
    private final PacienteRepository pacienteRepository;
    private final HistoriaClinicaService historiaClinicaService;
    private final CitaRepository citaRepository;

    private static final List<CondicionOdontograma> CONDICIONES_CATALOGO_COMPLETO = List.of(
        new CondicionOdontograma("CARIES", "Caries Dental", "Lesión cariosa activa o recidivante", "#EF4444", true),
        new CondicionOdontograma("RESTAURACION", "Restauración / Obturación", "Resina compuesta, amalgama o ionómero", "#3B82F6", true),
        new CondicionOdontograma("CORONA", "Corona Protésica", "Corona fija o funda dental protésica", "#A855F7", true),
        new CondicionOdontograma("AUSENTE", "Pieza Ausente", "Pieza dental extraída o ausente", "#64748B", true),
        new CondicionOdontograma("ENDODONCIA", "Endodoncia / Conducto", "Tratamiento pulpar o conducto realizado", "#F97316", true),
        new CondicionOdontograma("SELLANTE", "Sellante de Fosas", "Sellado preventivo de fosas y fisuras", "#06B6D4", true),
        new CondicionOdontograma("FRACTURA", "Fractura / Fisura", "Pérdida de tejido dentario por traumatismo o fisura", "#E11D48", true),
        new CondicionOdontograma("IMPLANTE", "Implante Dental", "Implante osteointegrado o prótesis sobre implante", "#8B5CF6", true),
        new CondicionOdontograma("PROTESIS_REMOVIBLE", "Prótesis Removible", "Aparato protésico removible parcial o total", "#F59E0B", true),
        new CondicionOdontograma("DIENTE_IMPACTADO", "Diente Impactado / Retenido", "Pieza dental no erupcionada retenida en hueso", "#EC4899", true),
        new CondicionOdontograma("SANO", "Sano / Normal", "Pieza íntegra sin hallazgos patológicos", "#10B981", true)
    );

    @Transactional
    public List<CondicionOdontogramaResponse> listarCondiciones() {
        // Asegurar que todas las condiciones clínicas estándar existan en MySQL
        for (CondicionOdontograma def : CONDICIONES_CATALOGO_COMPLETO) {
            if (condicionRepository.findByCodigoIgnoreCase(def.getCodigo()).isEmpty()) {
                try {
                    condicionRepository.save(new CondicionOdontograma(
                        def.getCodigo(), def.getNombre(), def.getDescripcion(), def.getColorHexadecimal(), true
                    ));
                } catch (Exception ex) {
                    log.debug("Condición {} no pudo guardarse o ya existe: {}", def.getCodigo(), ex.getMessage());
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
                c.getActivo()
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

        CondicionOdontograma nueva = new CondicionOdontograma(
            codigo,
            request.nombre().trim(),
            request.descripcion() != null && !request.descripcion().isBlank() ? request.descripcion().trim() : null,
            color,
            true
        );

        CondicionOdontograma guardada = condicionRepository.save(nueva);
        log.info("Nueva condición dental creada por usuario: ID={}, Código={}, Nombre={}",
            guardada.getId(), guardada.getCodigo(), guardada.getNombre());

        return new CondicionOdontogramaResponse(
            guardada.getId(),
            guardada.getCodigo(),
            guardada.getNombre(),
            guardada.getDescripcion(),
            guardada.getColorHexadecimal(),
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
                condicionRepository.save(new CondicionOdontograma(def.getCodigo(), def.getNombre(), def.getDescripcion(), def.getColorHexadecimal(), true))
            );
        }
        if (condicion == null) {
            throw new ResourceNotFoundException("Condición dental no encontrada con ID: " + request.idCondicion());
        }

        String sup = normalizarSuperficie(request.superficie());
        Byte piezaByte = request.numeroPieza() != null ? request.numeroPieza().byteValue() : null;

        // Si la condición es "SANO", removemos cualquier hallazgo patológico previo en esa superficie/pieza
        if ("SANO".equalsIgnoreCase(condicion.getCodigo())) {
            if (sup == null || "GENERAL".equalsIgnoreCase(sup)) {
                detalleRepository.deleteByOdontogramaIdAndNumeroPieza(odontograma.getId(), piezaByte);
            } else {
                detalleRepository.deleteByOdontogramaIdAndNumeroPiezaAndSuperficie(odontograma.getId(), piezaByte, sup);
            }
        } else {
            // Si la nueva condición es AUSENTE o CORONA (condición de pieza completa), limpia superficies individuales
            if ("AUSENTE".equalsIgnoreCase(condicion.getCodigo()) || "CORONA".equalsIgnoreCase(condicion.getCodigo()) || sup == null || "GENERAL".equalsIgnoreCase(sup)) {
                detalleRepository.deleteByOdontogramaIdAndNumeroPieza(odontograma.getId(), piezaByte);
                sup = "GENERAL";
            } else {
                // Si es por superficie, elimina hallazgo previo de pieza completa o de la misma superficie
                detalleRepository.deleteByOdontogramaIdAndNumeroPiezaAndSuperficie(odontograma.getId(), piezaByte, "GENERAL");
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
