package com.neodent.historia.service;

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

    @Transactional(readOnly = true)
    public Optional<HistoriaClinicaResponse> obtenerPorPacienteId(Long pacienteId) {
        return historiaRepository.findByPacienteIdWithDetails(pacienteId)
            .map(this::mapearResponse);
    }

    @Transactional
    public HistoriaClinicaResponse crearHistoriaClinica(Long pacienteId, GuardarHistoriaClinicaRequest request) {
        if (historiaRepository.existsByPacienteId(pacienteId)) {
            throw new BusinessException("El paciente ya cuenta con una historia clínica registrada.");
        }

        Paciente paciente = pacienteRepository.findById(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con ID: " + pacienteId));

        HistoriaClinica nueva = new HistoriaClinica(
            paciente,
            request.antecedentes() != null ? request.antecedentes().trim() : null,
            request.alergias() != null ? request.alergias().trim() : null,
            request.observacionesGenerales() != null ? request.observacionesGenerales().trim() : null
        );

        HistoriaClinica guardada = historiaRepository.save(nueva);
        log.info("Historia clínica creada exitosamente para paciente ID: {}", pacienteId);

        return mapearResponse(guardada);
    }

    @Transactional
    public HistoriaClinicaResponse actualizarHistoriaClinica(Long pacienteId, GuardarHistoriaClinicaRequest request) {
        HistoriaClinica historia = historiaRepository.findByPacienteId(pacienteId)
            .orElseThrow(() -> new ResourceNotFoundException("No se encontró la historia clínica para el paciente ID: " + pacienteId));

        if (request.antecedentes() != null) {
            historia.setAntecedentes(request.antecedentes().trim());
        }
        if (request.alergias() != null) {
            historia.setAlergias(request.alergias().trim());
        }
        if (request.observacionesGenerales() != null) {
            historia.setObservacionesGenerales(request.observacionesGenerales().trim());
        }

        HistoriaClinica actualizada = historiaRepository.save(historia);
        log.info("Historia clínica actualizada para paciente ID: {}", pacienteId);

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
        // Fallback al primer odontólogo activo disponible (ej. si actúa como ADMIN)
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

        return new HistoriaClinicaResponse(
            h.getId(),
            p.getId(),
            nombre,
            tipoDoc,
            p.getNumeroDocumento(),
            p.getFechaNacimiento(),
            edad,
            h.getAntecedentes(),
            h.getAlergias(),
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
