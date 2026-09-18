package com.neodent.cita.service;

import com.neodent.cita.dto.request.CrearReservaCitaRequest;
import com.neodent.cita.dto.response.ReservaCitaResponse;
import com.neodent.cita.model.ReservaCitaTemporal;
import com.neodent.cita.repository.BloqueoHorarioRepository;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.cita.repository.HorarioOdontologoRepository;
import com.neodent.cita.repository.ReservaCitaTemporalRepository;
import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.sede.model.Sede;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.servicio.model.Servicio;
import com.neodent.servicio.repository.ServicioRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaCitaService {

    private final ReservaCitaTemporalRepository reservaRepository;
    private final CitaRepository citaRepository;
    private final BloqueoHorarioRepository bloqueoRepository;
    private final PacienteRepository pacienteRepository;
    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;
    private final SedeRepository sedeRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioOdontologoRepository horarioRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public ReservaCitaResponse crearHold(CrearReservaCitaRequest request, Long usuarioId, List<String> roles) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        if (!request.fechaHoraInicio().isAfter(ahora)) {
            throw new ConflictException("La cita debe programarse en una fecha futura");
        }

        Paciente paciente;
        if (roles != null && roles.contains(AppConstants.Roles.PACIENTE)) {
            paciente = pacienteRepository.findByUsuarioIdAndActivoTrue(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente asociado al usuario no encontrado"));
        } else {
            if (request.pacienteId() == null) throw new ConflictException("Debe seleccionar un paciente");
            paciente = pacienteRepository.findById(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
        }

        OdontologoEspecialidad oe = odontologoEspecialidadRepository.findByIdAndActivoTrue(request.odontologoEspecialidadId())
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo/especialidad no disponible"));

        Sede sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
            .orElseThrow(() -> new ResourceNotFoundException("Sede no disponible"));

        Servicio servicio = null;
        int duracionMinutos = AppConstants.Citas.DURACION_DEFAULT_MINUTOS;

        if (request.servicioId() != null) {
            servicio = servicioRepository.findByIdAndActivoTrue(request.servicioId())
                .orElseThrow(() -> new ResourceNotFoundException("Servicio no disponible"));

            if (!servicio.getEspecialidad().getId().equals(oe.getEspecialidad().getId())) {
                throw new ConflictException("El servicio no pertenece a la especialidad seleccionada");
            }

            duracionMinutos = servicio.getDuracionMinutos();
        }

        LocalDateTime inicio = request.fechaHoraInicio();
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
        Long odontologoId = oe.getOdontologo().getId();

        boolean bloqueado = bloqueoRepository.contarBloqueos(odontologoId, sede.getId(), inicio, fin) > 0;
        if (bloqueado) {
            throw new ConflictException("El odontólogo no está disponible en ese horario");
        }

        byte diaSemana = (byte) inicio.getDayOfWeek().getValue();
        boolean dentroHorario = horarioRepository.existeHorarioDisponible(
            oe.getId(),
            sede.getId(),
            diaSemana,
            inicio.toLocalTime(),
            fin.toLocalTime()
        );

        if (!dentroHorario) {
            throw new ConflictException("El horario seleccionado está fuera del horario de atención del odontólogo");
        }

        if (citaRepository.existeCitaActivaEnHorario(odontologoId, inicio, fin)) {
            throw new ConflictException("El horario ya se encuentra ocupado");
        }

        if (reservaRepository.existeReservaVigente(odontologoId, inicio, fin, ahora)) {
            throw new ConflictException("El horario está siendo reservado por otro usuario");
        }

        String token = generarToken();
        LocalDateTime expiresAt = ahora.plusMinutes(AppConstants.Citas.HOLD_MINUTOS);

        ReservaCitaTemporal reserva = new ReservaCitaTemporal();
        reserva.setTokenReserva(token);
        reserva.setPaciente(paciente);
        reserva.setOdontologoEspecialidad(oe);
        reserva.setSede(sede);
        reserva.setServicioSolicitado(servicio);
        reserva.setFechaHoraInicio(inicio);
        reserva.setFechaHoraFin(fin);
        reserva.setFechaExpiracion(expiresAt);
        reserva.setConfirmada(false);

        reservaRepository.save(reserva);

        return new ReservaCitaResponse(
            token,
            inicio,
            fin,
            expiresAt,
            Duration.between(ahora, expiresAt).getSeconds(),
            "Horario reservado temporalmente"
        );
    }

    private String generarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}