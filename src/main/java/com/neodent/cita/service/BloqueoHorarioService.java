package com.neodent.cita.service;

import com.neodent.cita.dto.request.CrearBloqueoHorarioRequest;
import com.neodent.cita.dto.response.BloqueoHorarioResponse;
import com.neodent.cita.model.BloqueoHorario;
import com.neodent.cita.model.TipoBloqueoHorario;
import com.neodent.cita.repository.BloqueoHorarioRepository;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.cita.repository.TipoBloqueoHorarioRepository;
import com.neodent.odontologo.model.Odontologo;
import com.neodent.odontologo.repository.OdontologoRepository;
import com.neodent.sede.model.Sede;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueoHorarioService {

    private final BloqueoHorarioRepository bloqueoRepository;
    private final TipoBloqueoHorarioRepository tipoBloqueoRepository;
    private final OdontologoRepository odontologoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;
    private final CitaRepository citaRepository;
    private final Clock clock;

    @Transactional
    public BloqueoHorarioResponse crear(
        CrearBloqueoHorarioRequest request,
        Long usuarioId
    ) {
        LocalDateTime ahora =
            LocalDateTime.now(clock);

        if (
            !request.fechaFin()
                .isAfter(request.fechaInicio())
        ) {
            throw new ConflictException(
                "La fecha fin debe ser posterior a la fecha inicio"
            );
        }

        if (
            request.fechaInicio()
                .isBefore(ahora)
        ) {
            throw new ConflictException(
                "No se puede crear un bloqueo en una fecha u hora pasada"
            );
        }

        TipoBloqueoHorario tipo =
            tipoBloqueoRepository
                .findByIdAndActivoTrue(
                    request.tipoBloqueoId()
                )
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Tipo de bloqueo no encontrado o inactivo"
                    )
                );

        validarAlcance(
            tipo,
            request
        );

        Odontologo odontologo = null;

        if (
            request.odontologoId() != null
        ) {
            odontologo =
                odontologoRepository
                    .findByIdAndActivoTrue(
                        request.odontologoId()
                    )
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Odontólogo no encontrado"
                        )
                    );
        }

        Sede sede = null;

        if (
            request.sedeId() != null
        ) {
            sede =
                sedeRepository
                    .findByIdAndActivoTrue(
                        request.sedeId()
                    )
                    .orElseThrow(() ->
                        new ResourceNotFoundException(
                            "Sede no encontrada"
                        )
                    );
        }

        Usuario usuario =
            usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Usuario autenticado no encontrado"
                    )
                );

        boolean cruce =
            bloqueoRepository
                .existeCruceBloqueo(
                    request.odontologoId(),
                    request.sedeId(),
                    request.fechaInicio(),
                    request.fechaFin()
                );

        if (cruce) {
            throw new ConflictException(
                "Existe otro bloqueo que se cruza con el intervalo indicado"
            );
        }

        validarCitasExistentes(
            request
        );

        BloqueoHorario bloqueo =
            new BloqueoHorario();

        bloqueo.setTipoBloqueo(tipo);
        bloqueo.setOdontologo(odontologo);
        bloqueo.setSede(sede);

        bloqueo.setFechaInicio(
            request.fechaInicio()
        );

        bloqueo.setFechaFin(
            request.fechaFin()
        );

        bloqueo.setMotivo(
            normalizarTexto(
                request.motivo()
            )
        );

        bloqueo.setCreadoPor(usuario);

        return mapear(
            bloqueoRepository.save(bloqueo)
        );
    }

    @Transactional
    public BloqueoHorarioResponse actualizar(
        Long id,
        CrearBloqueoHorarioRequest request,
        Long usuarioId
    ) {
        BloqueoHorario bloqueo = bloqueoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));

        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new ConflictException("La fecha fin debe ser posterior a la fecha inicio");
        }

        TipoBloqueoHorario tipo = tipoBloqueoRepository.findByIdAndActivoTrue(request.tipoBloqueoId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de bloqueo no encontrado o inactivo"));

        validarAlcance(tipo, request);

        Odontologo odontologo = null;
        if (request.odontologoId() != null) {
            odontologo = odontologoRepository.findByIdAndActivoTrue(request.odontologoId())
                .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));
        }

        Sede sede = null;
        if (request.sedeId() != null) {
            sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        boolean cruce = bloqueoRepository.existeCruceBloqueoExcluyendo(
            id,
            request.odontologoId(),
            request.sedeId(),
            request.fechaInicio(),
            request.fechaFin()
        );

        if (cruce) {
            throw new ConflictException("Existe otro bloqueo que se cruza con el intervalo indicado");
        }

        validarCitasExistentes(request);

        bloqueo.setTipoBloqueo(tipo);
        bloqueo.setOdontologo(odontologo);
        bloqueo.setSede(sede);
        bloqueo.setFechaInicio(request.fechaInicio());
        bloqueo.setFechaFin(request.fechaFin());
        bloqueo.setMotivo(normalizarTexto(request.motivo()));
        bloqueo.setCreadoPor(usuario);

        return mapear(bloqueoRepository.save(bloqueo));
    }

    @Transactional
    public void eliminar(Long id) {
        BloqueoHorario bloqueo =
            bloqueoRepository
                .findById(id)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Bloqueo no encontrado"
                    )
                );

        bloqueoRepository.delete(bloqueo);
    }

    @Transactional(readOnly = true)
    public List<BloqueoHorarioResponse> listar(
        Long odontologoId,
        Integer sedeId
    ) {
        return bloqueoRepository
            .listarFiltrado(
                odontologoId,
                sedeId
            )
            .stream()
            .map(this::mapear)
            .toList();
    }

    private void validarAlcance(
        TipoBloqueoHorario tipo,
        CrearBloqueoHorarioRequest request
    ) {
        if (
            Boolean.TRUE.equals(
                tipo.getRequiereOdontologo()
            )
            && request.odontologoId() == null
        ) {
            throw new ConflictException(
                "El tipo de bloqueo seleccionado requiere un odontólogo"
            );
        }

        if (
            Boolean.FALSE.equals(
                tipo.getPermiteOdontologo()
            )
            && request.odontologoId() != null
        ) {
            throw new ConflictException(
                "El tipo de bloqueo seleccionado no permite asociar un odontólogo"
            );
        }

        if (
            Boolean.TRUE.equals(
                tipo.getRequiereSede()
            )
            && request.sedeId() == null
        ) {
            throw new ConflictException(
                "El tipo de bloqueo seleccionado requiere una sede"
            );
        }

        if (
            Boolean.FALSE.equals(
                tipo.getPermiteSede()
            )
            && request.sedeId() != null
        ) {
            throw new ConflictException(
                "El tipo de bloqueo seleccionado no permite asociar una sede"
            );
        }
    }

    private void validarCitasExistentes(CrearBloqueoHorarioRequest request) {
        if (request.odontologoId() != null) {
            boolean tieneCitas = request.sedeId() != null
                ? citaRepository.existeCitaActivaEnHorarioYSede(
                    request.odontologoId(),
                    request.sedeId(),
                    request.fechaInicio(),
                    request.fechaFin()
                )
                : citaRepository.existeCitaActivaEnHorario(
                    request.odontologoId(),
                    request.fechaInicio(),
                    request.fechaFin()
                );

            if (tieneCitas) {
                throw new ConflictException(
                    request.sedeId() == null
                        ? "No se puede crear el bloqueo porque el odontólogo tiene citas programadas en ese intervalo"
                        : "No se puede crear el bloqueo porque el odontólogo tiene citas programadas en esa sede durante el intervalo indicado"
                );
            }

            return;
        }

        boolean tieneCitas = request.sedeId() != null
            ? citaRepository.existeCitaActivaGlobalEnHorarioYSede(
                request.sedeId(),
                request.fechaInicio(),
                request.fechaFin()
            )
            : citaRepository.existeCitaActivaGlobalEnHorario(
                request.fechaInicio(),
                request.fechaFin()
            );

        if (tieneCitas) {
            throw new ConflictException(
                request.sedeId() == null
                    ? "No se puede registrar el cierre porque existen citas programadas en ese intervalo"
                    : "No se puede cerrar la sede porque existen citas programadas en ese intervalo"
            );
        }
    }

    private String normalizarTexto(
        String valor
    ) {
        if (
            valor == null
            || valor.isBlank()
        ) {
            return null;
        }

        return valor.trim();
    }

    private BloqueoHorarioResponse mapear(
        BloqueoHorario bloqueo
    ) {
        return new BloqueoHorarioResponse(
            bloqueo.getId(),

            bloqueo
                .getTipoBloqueo()
                .getId(),

            bloqueo
                .getTipoBloqueo()
                .getCodigo(),

            bloqueo
                .getTipoBloqueo()
                .getNombre(),

            bloqueo.getOdontologo() != null
                ? bloqueo
                    .getOdontologo()
                    .getId()
                : null,

            bloqueo.getSede() != null
                ? bloqueo
                    .getSede()
                    .getId()
                : null,

            bloqueo.getFechaInicio(),
            bloqueo.getFechaFin(),
            bloqueo.getMotivo(),
            bloqueo.getCreadoPor().getId(),
            bloqueo.getFechaCreacion()
        );
    }
}