package com.neodent.cita.service;

import com.neodent.cita.dto.request.ActualizarHorarioOdontologoRequest;
import com.neodent.cita.dto.request.CrearHorarioOdontologoRequest;
import com.neodent.cita.dto.response.HorarioOdontologoResponse;
import com.neodent.cita.model.HorarioOdontologo;
import com.neodent.cita.repository.HorarioOdontologoRepository;
import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.sede.model.Sede;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioOdontologoService {

    private final HorarioOdontologoRepository horarioRepository;
    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;
    private final SedeRepository sedeRepository;

    @Transactional
    public HorarioOdontologoResponse crear(CrearHorarioOdontologoRequest request) {
        validarHoras(request.horaInicio(), request.horaFin());

        OdontologoEspecialidad oe = odontologoEspecialidadRepository
            .findByIdAndActivoTrue(request.odontologoEspecialidadId())
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo/especialidad no encontrado"));

        Sede sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
            .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        boolean cruce = horarioRepository.existeCruceHorario(
            null,
            oe.getOdontologo().getId(),
            request.diaSemana(),
            request.horaInicio(),
            request.horaFin()
        );

        if (cruce) {
            throw new ConflictException("El horario se cruza con otro horario activo");
        }

        HorarioOdontologo horario = new HorarioOdontologo();
        horario.setOdontologoEspecialidad(oe);
        horario.setSede(sede);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setActivo(true);

        return mapear(horarioRepository.save(horario));
    }

    @Transactional
    public HorarioOdontologoResponse actualizar(Long id, ActualizarHorarioOdontologoRequest request) {
        HorarioOdontologo horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));

        validarHoras(request.horaInicio(), request.horaFin());

        OdontologoEspecialidad oe = odontologoEspecialidadRepository.findByIdAndActivoTrue(request.odontologoEspecialidadId())
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo/especialidad no encontrado"));

        Sede sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
            .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        boolean cruce = horarioRepository.existeCruceHorario(
            id,
            oe.getOdontologo().getId(),
            request.diaSemana(),
            request.horaInicio(),
            request.horaFin()
        );

        if (cruce) {
            throw new ConflictException("El horario se cruza con otro horario activo");
        }

        horario.setOdontologoEspecialidad(oe);
        horario.setSede(sede);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setActivo(true);

        return mapear(horarioRepository.save(horario));
    }

    @Transactional
    public void desactivar(Long id) {
        HorarioOdontologo horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));

        horario.setActivo(false);
    }

    @Transactional(readOnly = true)
    public List<HorarioOdontologoResponse> listar(Long odontologoEspecialidadId) {
        List<HorarioOdontologo> horarios = odontologoEspecialidadId == null
            ? horarioRepository.findByActivoTrueOrderByDiaSemanaAscHoraInicioAsc()
            : horarioRepository.findByOdontologoEspecialidadIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
                odontologoEspecialidadId
            );

        return horarios.stream().map(this::mapear).toList();
    }

    private void validarHoras(java.time.LocalTime inicio, java.time.LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ConflictException("La hora fin debe ser posterior a la hora inicio");
        }
    }

    private HorarioOdontologoResponse mapear(HorarioOdontologo horario) {
        return new HorarioOdontologoResponse(
            horario.getId(),
            horario.getOdontologoEspecialidad().getId(),
            horario.getSede().getId(),
            horario.getDiaSemana(),
            horario.getHoraInicio(),
            horario.getHoraFin(),
            horario.getActivo()
        );
    }
}