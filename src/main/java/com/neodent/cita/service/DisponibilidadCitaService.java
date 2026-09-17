package com.neodent.cita.service;

import com.neodent.cita.dto.response.DisponibilidadCitaResponse;
import com.neodent.cita.dto.response.HorarioDisponibleResponse;
import com.neodent.cita.model.HorarioOdontologo;
import com.neodent.cita.repository.BloqueoHorarioRepository;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.cita.repository.HorarioOdontologoRepository;
import com.neodent.cita.repository.ReservaCitaTemporalRepository;
import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.servicio.model.Servicio;
import com.neodent.servicio.repository.ServicioRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DisponibilidadCitaService {

    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;
    private final SedeRepository sedeRepository;
    private final ServicioRepository servicioRepository;
    private final HorarioOdontologoRepository horarioRepository;
    private final BloqueoHorarioRepository bloqueoRepository;
    private final CitaRepository citaRepository;
    private final ReservaCitaTemporalRepository reservaRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DisponibilidadCitaResponse obtenerDisponibilidad(
        Long odontologoEspecialidadId,
        Integer sedeId,
        Integer servicioId,
        LocalDate fecha
    ) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        if (fecha.isBefore(ahora.toLocalDate())) {
            throw new ConflictException("La fecha debe ser actual o futura");
        }

        OdontologoEspecialidad oe = odontologoEspecialidadRepository.findByIdAndActivoTrue(odontologoEspecialidadId)
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo/especialidad no disponible"));

        sedeRepository.findByIdAndActivoTrue(sedeId)
            .orElseThrow(() -> new ResourceNotFoundException("Sede no disponible"));

        Servicio servicio = servicioRepository.findByIdAndActivoTrue(servicioId)
            .orElseThrow(() -> new ResourceNotFoundException("Servicio no disponible"));

        if (!servicio.getEspecialidad().getId().equals(oe.getEspecialidad().getId())) {
            throw new ConflictException("El servicio no pertenece a la especialidad seleccionada");
        }

        int duracion = servicio.getDuracionMinutos() != null ? servicio.getDuracionMinutos() : AppConstants.Citas.DURACION_DEFAULT_MINUTOS;
        int diaSemana = fecha.getDayOfWeek().getValue();

        List<HorarioOdontologo> horarios = horarioRepository
            .findByOdontologoEspecialidadIdAndSedeIdAndDiaSemanaAndActivoTrueOrderByHoraInicio(odontologoEspecialidadId, sedeId, diaSemana);

        Long odontologoId = oe.getOdontologo().getId();
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();

        var citasDelDia = citaRepository.buscarCitasActivasDelDia(odontologoId, inicioDia, finDia);
        var bloqueosDelDia = bloqueoRepository.buscarBloqueosDelDia(odontologoId, sedeId, inicioDia, finDia);
        var reservasDelDia = reservaRepository.buscarReservasVigentesDelDia(odontologoId, inicioDia, finDia, ahora);

        List<HorarioDisponibleResponse> disponibles = new ArrayList<>();
        Set<LocalTime> iniciosAgregados = new LinkedHashSet<>();

        for (HorarioOdontologo horario : horarios) {
            LocalTime cursor = horario.getHoraInicio();

            while (!cursor.plusMinutes(duracion).isAfter(horario.getHoraFin())) {
                LocalTime horaFin = cursor.plusMinutes(duracion);
                LocalDateTime inicio = LocalDateTime.of(fecha, cursor);
                LocalDateTime fin = LocalDateTime.of(fecha, horaFin);

                if (!inicio.isAfter(ahora)) {
                    cursor = cursor.plusMinutes(AppConstants.Citas.INTERVALO_SLOTS_MINUTOS);
                    continue;
                }

                boolean bloqueado = bloqueosDelDia.stream()
                    .anyMatch(b -> b.getFechaInicio().isBefore(fin) && b.getFechaFin().isAfter(inicio));

                boolean tieneCita = citasDelDia.stream()
                    .anyMatch(c -> c.getFechaHoraInicio().isBefore(fin) && c.getFechaHoraFin().isAfter(inicio));

                boolean tieneHold = reservasDelDia.stream()
                    .anyMatch(r -> r.getFechaHoraInicio().isBefore(fin) && r.getFechaHoraFin().isAfter(inicio));

                if (!bloqueado && !tieneCita && !tieneHold && iniciosAgregados.add(cursor)) {
                    disponibles.add(new HorarioDisponibleResponse(cursor, horaFin));
                }

                cursor = cursor.plusMinutes(AppConstants.Citas.INTERVALO_SLOTS_MINUTOS);
            }
        }

        return new DisponibilidadCitaResponse(odontologoEspecialidadId, sedeId, servicioId, fecha, duracion, disponibles);
    }
}