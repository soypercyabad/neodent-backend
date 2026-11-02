package com.neodent.historia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neodent.audit.AuditService;
import com.neodent.cita.model.Cita;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.historia.dto.EvolucionClinicaDto;
import com.neodent.historia.dto.GuardarHistoriaClinicaRequest;
import com.neodent.historia.dto.HistoriaClinicaResponse;
import com.neodent.historia.model.EvolucionClinica;
import com.neodent.historia.model.HistoriaClinica;
import com.neodent.historia.repository.EvolucionClinicaRepository;
import com.neodent.historia.repository.HistoriaClinicaRepository;
import com.neodent.odontograma.repository.OdontogramaRepository;
import com.neodent.odontologo.model.Odontologo;
import com.neodent.odontologo.repository.OdontologoRepository;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.personal.model.Personal;
import com.neodent.personal.repository.PersonalRepository;
import com.neodent.shared.exception.BusinessException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoriaClinicaService {

    private final HistoriaClinicaRepository historiaRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontogramaRepository odontogramaRepository;
    private final EvolucionClinicaRepository evolucionRepository;
    private final PersonalRepository personalRepository;
    private final OdontologoRepository odontologoRepository;
    private final CitaRepository citaRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional(readOnly = true)
    public Optional<HistoriaClinicaResponse> obtenerPorPacienteId(Long pacienteId) {
        return historiaRepository.findByPacienteIdWithDetails(pacienteId)
            .map(this::mapearResponse);
    }

    @Transactional
    public HistoriaClinicaResponse crearHistoriaClinica(Long pacienteId, GuardarHistoriaClinicaRequest request, Long usuarioId, String ip) {
        if (historiaRepository.existsByPacienteId(pacienteId)) {
            throw new BusinessException("El paciente ya cuenta con una historia clínica registrada.");
        }

        Paciente paciente = pacienteRepository.findById(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + pacienteId));

        String antMed = request.antecedentesMedicos() != null ? request.antecedentesMedicos().trim() : request.antecedentes();

        HistoriaClinica nueva = new HistoriaClinica(
            paciente,
            antMed != null ? antMed.trim() : null,
            request.alergias() != null ? request.alergias().trim() : null,
            request.observacionesGenerales() != null ? request.observacionesGenerales().trim() : null
        );

        nueva.setMotivoConsulta(request.motivoConsulta() != null ? request.motivoConsulta().trim() : null);
        nueva.setEnfermedadActual(request.enfermedadActual() != null ? request.enfermedadActual().trim() : null);
        nueva.setAntecedentesMedicos(antMed != null ? antMed.trim() : null);
        nueva.setMedicamentosActuales(request.medicamentosActuales() != null ? request.medicamentosActuales().trim() : null);
        nueva.setHabitos(request.habitos() != null ? request.habitos().trim() : null);
        nueva.setAntecedentesOdontologicos(request.antecedentesOdontologicos() != null ? request.antecedentesOdontologicos().trim() : null);
        nueva.setExamenExtraoral(request.examenExtraoral() != null ? request.examenExtraoral().trim() : null);
        nueva.setExamenIntraoral(request.examenIntraoral() != null ? request.examenIntraoral().trim() : null);
        nueva.setDiagnostico(request.diagnostico() != null ? request.diagnostico().trim() : null);
        nueva.setPlanTratamiento(request.planTratamiento() != null ? request.planTratamiento().trim() : null);

        HistoriaClinica guardada = historiaRepository.save(nueva);
        log.info("Historia clínica creada exitosamente para paciente ID: {}", pacienteId);

        auditService.registrar(
            usuarioId,
            "CREAR",
            "historia_clinica",
            guardada.getId(),
            "Creación de historia clínica para paciente ID: " + pacienteId,
            ip,
            null
        );

        return mapearResponse(guardada);
    }

    @Transactional
    public HistoriaClinicaResponse actualizarHistoriaClinica(Long pacienteId, GuardarHistoriaClinicaRequest request, Long usuarioId, String ip) {
        HistoriaClinica historia = historiaRepository.findByPacienteId(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la historia clínica para el paciente ID: " + pacienteId));

        if (request.motivoConsulta() != null) historia.setMotivoConsulta(request.motivoConsulta().trim());
        if (request.enfermedadActual() != null) historia.setEnfermedadActual(request.enfermedadActual().trim());
        if (request.alergias() != null) historia.setAlergias(request.alergias().trim());

        String antMed = request.antecedentesMedicos() != null ? request.antecedentesMedicos().trim() : request.antecedentes();
        if (antMed != null) {
            historia.setAntecedentesMedicos(antMed.trim());
            historia.setAntecedentes(antMed.trim());
        }

        if (request.medicamentosActuales() != null) historia.setMedicamentosActuales(request.medicamentosActuales().trim());
        if (request.habitos() != null) historia.setHabitos(request.habitos().trim());
        if (request.antecedentesOdontologicos() != null) historia.setAntecedentesOdontologicos(request.antecedentesOdontologicos().trim());
        if (request.examenExtraoral() != null) historia.setExamenExtraoral(request.examenExtraoral().trim());
        if (request.examenIntraoral() != null) historia.setExamenIntraoral(request.examenIntraoral().trim());
        if (request.diagnostico() != null) historia.setDiagnostico(request.diagnostico().trim());
        if (request.planTratamiento() != null) historia.setPlanTratamiento(request.planTratamiento().trim());
        if (request.observacionesGenerales() != null) historia.setObservacionesGenerales(request.observacionesGenerales().trim());

        HistoriaClinica actualizada = historiaRepository.save(historia);
        log.info("Historia clínica actualizada para paciente ID: {}", pacienteId);

        auditService.registrar(
            usuarioId,
            "ACTUALIZAR",
            "historia_clinica",
            actualizada.getId(),
            "Actualización de historia clínica para paciente ID: " + pacienteId,
            ip,
            null
        );

        return mapearResponse(actualizada);
    }

    @Transactional(readOnly = true)
    public List<EvolucionClinicaDto.Response> listarEvoluciones(Long pacienteId) {
        HistoriaClinica historia = historiaRepository.findByPacienteId(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("Historia clínica no encontrada para paciente ID: " + pacienteId));

        return evolucionRepository.findByHistoriaClinicaIdWithOdontologo(historia.getId())
            .stream()
            .map(this::mapearEvolucion)
            .toList();
    }

    @Transactional
    public EvolucionClinicaDto.Response agregarEvolucion(Long pacienteId, EvolucionClinicaDto.Request request, Long usuarioId) {
        HistoriaClinica historia = historiaRepository.findByPacienteId(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("Historia clínica no encontrada para paciente ID: " + pacienteId));

        Odontologo odontologo = resolverOdontologoPorUsuario(usuarioId);

        Cita cita = null;
        if (request.citaId() != null) {
            cita = citaRepository.findById(request.citaId()).orElse(null);
        }

        EvolucionClinica evolucion = new EvolucionClinica(
            historia,
            cita,
            odontologo,
            request.diagnostico().trim(),
            request.procedimiento() != null ? request.procedimiento().trim() : null,
            request.indicaciones() != null ? request.indicaciones().trim() : null,
            request.observaciones() != null ? request.observaciones().trim() : null
        );

        EvolucionClinica guardada = evolucionRepository.save(evolucion);
        log.info("Evolución clínica registrada ID: {} para paciente ID: {}", guardada.getId(), pacienteId);

        return mapearEvolucion(guardada);
    }

    public Odontologo resolverOdontologoPorUsuario(Long usuarioId) {
        if (usuarioId != null) {
            Optional<Personal> personalOpt = personalRepository.findByUsuarioId(usuarioId);
            if (personalOpt.isPresent()) {
                Optional<Odontologo> odontologoOpt = odontologoRepository.findByPersonalId(personalOpt.get().getId());
                if (odontologoOpt.isPresent()) {
                    return odontologoOpt.get();
                }
            }
        }
        return odontologoRepository.findAll().stream()
            .filter(o -> Boolean.TRUE.equals(o.getActivo()))
            .findFirst()
            .orElseThrow(() -> new BusinessException("No se encontró un perfil de odontólogo activo asignado para esta operación."));
    }

    private HistoriaClinicaResponse mapearResponse(HistoriaClinica h) {
        Paciente p = h.getPaciente();
        String nombre = String.format("%s %s %s",
            p.getNombres(),
            p.getApellidoPaterno(),
            p.getApellidoMaterno() != null ? p.getApellidoMaterno() : ""
        ).trim();

        String tipoDoc = (p.getTipoDocumento() != null) ? p.getTipoDocumento().getCodigo() : "DNI";
        Integer edad = null;
        if (p.getFechaNacimiento() != null) {
            edad = Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears();
        }

        boolean tieneOdontograma = odontogramaRepository.findFirstByHistoriaClinicaIdOrderByFechaDesc(h.getId()).isPresent();
        int totalEvoluciones = evolucionRepository.findByHistoriaClinicaIdWithOdontologo(h.getId()).size();

        // Valores estructurados con fallback a JSON en observacionesGenerales para compatibilidad
        String motivo = h.getMotivoConsulta();
        String enfActual = h.getEnfermedadActual();
        String antMed = h.getAntecedentesMedicos() != null ? h.getAntecedentesMedicos() : h.getAntecedentes();
        String medAct = h.getMedicamentosActuales();
        String hab = h.getHabitos();
        String antOdon = h.getAntecedentesOdontologicos();
        String exExt = h.getExamenExtraoral();
        String exInt = h.getExamenIntraoral();
        String diag = h.getDiagnostico();
        String planTrat = h.getPlanTratamiento();

        if (h.getObservacionesGenerales() != null && h.getObservacionesGenerales().trim().startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(h.getObservacionesGenerales());
                if (motivo == null && root.hasNonNull("motivoConsulta")) motivo = root.get("motivoConsulta").asText();
                if (enfActual == null && root.hasNonNull("enfermedadActual")) enfActual = root.get("enfermedadActual").asText();
                if (antMed == null && root.hasNonNull("antecedentesMedicos")) antMed = root.get("antecedentesMedicos").asText();
                if (medAct == null && root.hasNonNull("medicamentosActuales")) medAct = root.get("medicamentosActuales").asText();
                if (hab == null && root.hasNonNull("habitos")) hab = root.get("habitos").asText();
                if (antOdon == null && root.hasNonNull("antecedentesOdontologicos")) antOdon = root.get("antecedentesOdontologicos").asText();
                if (exExt == null && root.hasNonNull("examenExtraoral")) exExt = root.get("examenExtraoral").asText();
                if (exInt == null && root.hasNonNull("examenIntraoral")) exInt = root.get("examenIntraoral").asText();
                if (diag == null && root.hasNonNull("diagnostico")) diag = root.get("diagnostico").asText();
                if (planTrat == null && root.hasNonNull("planTratamiento")) planTrat = root.get("planTratamiento").asText();
            } catch (Exception ex) {
                log.debug("No se pudo parsear JSON legacy de observacionesGenerales: {}", ex.getMessage());
            }
        }

        return new HistoriaClinicaResponse(
            h.getId(),
            p.getId(),
            nombre,
            tipoDoc,
            p.getNumeroDocumento(),
            p.getFechaNacimiento(),
            edad,
            motivo,
            enfActual,
            h.getAntecedentes(),
            h.getAlergias(),
            antMed,
            medAct,
            hab,
            antOdon,
            exExt,
            exInt,
            diag,
            planTrat,
            h.getObservacionesGenerales(),
            h.getFechaCreacion(),
            h.getFechaActualizacion(),
            tieneOdontograma,
            totalEvoluciones
        );
    }

    private EvolucionClinicaDto.Response mapearEvolucion(EvolucionClinica e) {
        String odNombre = "Odontólogo General";
        String colegiatura = "-";
        if (e.getOdontologo() != null) {
            colegiatura = e.getOdontologo().getNumeroColegiatura();
            if (e.getOdontologo().getPersonal() != null) {
                Personal p = e.getOdontologo().getPersonal();
                odNombre = String.format("Dr(a). %s %s", p.getNombres(), p.getApellidoPaterno());
            }
        }

        return new EvolucionClinicaDto.Response(
            e.getId(),
            e.getHistoriaClinica().getId(),
            e.getCita() != null ? e.getCita().getId() : null,
            e.getOdontologo() != null ? e.getOdontologo().getId() : null,
            odNombre,
            colegiatura,
            e.getFecha(),
            e.getDiagnostico(),
            e.getProcedimiento(),
            e.getIndicaciones(),
            e.getObservaciones()
        );
    }
}
