package com.neodent.cita.service;

import com.neodent.cita.dto.request.ActualizarHorarioOdontologoRequest;
import com.neodent.cita.dto.request.CrearHorarioOdontologoRequest;
import com.neodent.cita.dto.response.HorarioOdontologoCatalogoResponse;
import com.neodent.cita.dto.response.HorarioOdontologoResponse;
import com.neodent.cita.dto.response.HorarioResumenEstadosResponse;
import com.neodent.cita.model.HorarioOdontologo;
import com.neodent.cita.repository.HorarioOdontologoRepository;
import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.odontologo.repository.OdontologoRepository;
import com.neodent.sede.model.Sede;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HorarioOdontologoService {

    private static final LocalDate FECHA_MAXIMA = LocalDate.of(9999, 12, 31);

    private final HorarioOdontologoRepository horarioRepository;
    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;
    private final SedeRepository sedeRepository;
    private final OdontologoRepository odontologoRepository;

    @Transactional
    public HorarioOdontologoResponse crear(CrearHorarioOdontologoRequest request) {
        validarHoras(request.horaInicio(), request.horaFin());
        validarVigencia(request.fechaInicioVigencia(), request.fechaFinVigencia());

        OdontologoEspecialidad oe = odontologoEspecialidadRepository
            .findByIdAndActivoTrue(request.odontologoEspecialidadId())
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo/especialidad no encontrado"));

        Long odontologoId = oe.getOdontologo().getId();

        odontologoRepository.bloquearParaReserva(odontologoId)
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        Sede sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
            .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        boolean cruce = horarioRepository.existeCruceHorario(
            null,
            odontologoId,
            request.diaSemana(),
            request.horaInicio(),
            request.horaFin(),
            request.fechaInicioVigencia(),
            fechaFinComparacion(request.fechaFinVigencia())
        );

        if (cruce) {
            throw new ConflictException(
                "El odontólogo ya tiene un horario que se cruza con el día, las horas y el periodo de vigencia seleccionados. Revisa también sus horarios en otras sedes."
            );
        }

        HorarioOdontologo horario = new HorarioOdontologo();
        horario.setOdontologoEspecialidad(oe);
        horario.setSede(sede);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setFechaInicioVigencia(request.fechaInicioVigencia());
        horario.setFechaFinVigencia(request.fechaFinVigencia());
        horario.setActivo(true);

        try {
            return mapear(horarioRepository.saveAndFlush(horario));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se pudo registrar el horario porque existe otro horario incompatible para el odontólogo.");
        }
    }

    @Transactional
    public HorarioOdontologoResponse actualizar(Long id, ActualizarHorarioOdontologoRequest request) {
        HorarioOdontologo horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));

        validarHoras(request.horaInicio(), request.horaFin());
        validarVigencia(request.fechaInicioVigencia(), request.fechaFinVigencia());

        OdontologoEspecialidad oe = odontologoEspecialidadRepository
            .findByIdAndActivoTrue(request.odontologoEspecialidadId())
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo/especialidad no encontrado"));

        Long odontologoId = oe.getOdontologo().getId();

        odontologoRepository.bloquearParaReserva(odontologoId)
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        Sede sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
            .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));

        boolean cruce = horarioRepository.existeCruceHorario(
            id,
            odontologoId,
            request.diaSemana(),
            request.horaInicio(),
            request.horaFin(),
            request.fechaInicioVigencia(),
            fechaFinComparacion(request.fechaFinVigencia())
        );

        if (cruce) {
            throw new ConflictException(
                "El horario se cruza con otro horario activo del odontólogo dentro de la misma vigencia"
            );
        }

        horario.setOdontologoEspecialidad(oe);
        horario.setSede(sede);
        horario.setDiaSemana(request.diaSemana());
        horario.setHoraInicio(request.horaInicio());
        horario.setHoraFin(request.horaFin());
        horario.setFechaInicioVigencia(request.fechaInicioVigencia());
        horario.setFechaFinVigencia(request.fechaFinVigencia());
        horario.setActivo(true);

        try {
            return mapear(horarioRepository.saveAndFlush(horario));
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("No se pudo actualizar el horario porque existe otro horario incompatible para el odontólogo.");
        }
    }

    @Transactional
    public void desactivar(Long id) {
        HorarioOdontologo horario = horarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado"));

        horario.setActivo(false);
    }

    @Transactional(readOnly = true)
    public List<HorarioOdontologoCatalogoResponse> listarCatalogoOdontologos() {
        return odontologoEspecialidadRepository
            .listarActivosParaHorarios()
            .stream()
            .map(oe -> {
                var odontologo = oe.getOdontologo();
                var personal = odontologo.getPersonal();

                return new HorarioOdontologoCatalogoResponse(
                    oe.getId(),
                    odontologo.getId(),
                    personal.getNombres(),
                    personal.getApellidoPaterno(),
                    personal.getApellidoMaterno(),
                    odontologo.getNumeroColegiatura(),
                    oe.getEspecialidad().getId(),
                    oe.getEspecialidad().getNombre(),
                    odontologo.getFotoNombreArchivo() != null && !odontologo.getFotoNombreArchivo().isBlank()
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<HorarioOdontologoResponse> listar(Long odontologoEspecialidadId) {
        return listar(odontologoEspecialidadId, null);
    }

    @Transactional(readOnly = true)
    public List<HorarioOdontologoResponse> listar(Long odontologoEspecialidadId, Long odontologoId) {
        List<HorarioOdontologo> horarios;
        if (odontologoEspecialidadId != null) {
            horarios = horarioRepository.findByOdontologoEspecialidadIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
                odontologoEspecialidadId
            );
        } else if (odontologoId != null) {
            horarios = horarioRepository.findByOdontologoEspecialidadOdontologoIdAndActivoTrueOrderByDiaSemanaAscHoraInicioAsc(
                odontologoId
            );
        } else {
            horarios = horarioRepository.findByActivoTrueOrderByDiaSemanaAscHoraInicioAsc();
        }

        return horarios.stream().map(this::mapear).toList();
    }

    @Transactional(readOnly = true)
    public Page<HorarioOdontologoResponse> listarPaginado(
        Long odontologoEspecialidadId,
        Long odontologoId,
        Integer especialidadId,
        Integer sedeId,
        Byte diaSemana,
        String estado,
        String buscar,
        int page,
        int size,
        String sortBy,
        String direction
    ) {
        String estadoNormalizado = null;
        if (estado != null && !estado.isBlank()) {
            String e = estado.trim().toUpperCase();
            if (e.startsWith("VIGENTE")) {
                estadoNormalizado = "VIGENTE";
            } else if (e.startsWith("PROXIMO")) {
                estadoNormalizado = "PROXIMO";
            } else if (e.startsWith("EXPIRADO") || e.startsWith("PASADO")) {
                estadoNormalizado = "EXPIRADO";
            } else {
                estadoNormalizado = e;
            }
        }
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;

        String campoOrden = switch (sortBy != null ? sortBy.toLowerCase() : "id") {
            case "odontologo" -> "odontologoEspecialidad.odontologo.personal.nombres";
            case "especialidad" -> "odontologoEspecialidad.especialidad.nombre";
            case "sede" -> "sede.nombre";
            case "diasemana" -> "diaSemana";
            case "horainicio" -> "horaInicio";
            case "fechainiciovigencia" -> "fechaInicioVigencia";
            default -> "id";
        };

        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        PageRequest pageable = PageRequest.of(page, size, Sort.by(sortDirection, campoOrden));

        return horarioRepository.buscarPaginado(
            odontologoEspecialidadId,
            odontologoId,
            especialidadId,
            sedeId,
            diaSemana,
            estadoNormalizado,
            buscarNormalizado,
            pageable
        ).map(this::mapear);
    }

    @Transactional(readOnly = true)
    public HorarioResumenEstadosResponse resumenEstados(
        Long odontologoEspecialidadId,
        Long odontologoId,
        Integer especialidadId,
        Integer sedeId,
        Byte diaSemana,
        String buscar
    ) {
        String buscarNormalizado = buscar != null && !buscar.isBlank() ? buscar.trim() : null;
        List<Object[]> resultados = horarioRepository.contarPorEstados(
            odontologoEspecialidadId,
            odontologoId,
            especialidadId,
            sedeId,
            diaSemana,
            buscarNormalizado
        );

        if (resultados.isEmpty() || resultados.get(0) == null) {
            return new HorarioResumenEstadosResponse(0L, 0L, 0L, 0L);
        }

        Object[] fila = resultados.get(0);
        long todos = fila[0] instanceof Number n ? n.longValue() : 0L;
        long vigentes = fila[1] instanceof Number n ? n.longValue() : 0L;
        long proximos = fila[2] instanceof Number n ? n.longValue() : 0L;
        long expirados = fila[3] instanceof Number n ? n.longValue() : 0L;

        return new HorarioResumenEstadosResponse(todos, vigentes, proximos, expirados);
    }

    private void validarHoras(LocalTime inicio, LocalTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ConflictException("La hora fin debe ser posterior a la hora inicio");
        }
    }

    private void validarVigencia(LocalDate inicio, LocalDate fin) {
        if (fin != null && fin.isBefore(inicio)) {
            throw new ConflictException("La fecha fin de vigencia no puede ser anterior a la fecha inicio");
        }
    }

    private LocalDate fechaFinComparacion(LocalDate fechaFinVigencia) {
        return fechaFinVigencia != null ? fechaFinVigencia : FECHA_MAXIMA;
    }

    private HorarioOdontologoResponse mapear(HorarioOdontologo horario) {
        return new HorarioOdontologoResponse(
            horario.getId(),
            horario.getOdontologoEspecialidad().getId(),
            horario.getSede().getId(),
            horario.getDiaSemana(),
            horario.getHoraInicio(),
            horario.getHoraFin(),
            horario.getFechaInicioVigencia(),
            horario.getFechaFinVigencia(),
            horario.getActivo()
        );
    }
}
