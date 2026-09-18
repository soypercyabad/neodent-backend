package com.neodent.cita.service;

import com.neodent.cita.dto.request.CrearBloqueoHorarioRequest;
import com.neodent.cita.dto.response.BloqueoHorarioResponse;
import com.neodent.cita.model.BloqueoHorario;
import com.neodent.cita.repository.BloqueoHorarioRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloqueoHorarioService {

    private final BloqueoHorarioRepository bloqueoRepository;
    private final OdontologoRepository odontologoRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public BloqueoHorarioResponse crear(CrearBloqueoHorarioRequest request, Long usuarioId) {
        if (!request.fechaFin().isAfter(request.fechaInicio())) {
            throw new ConflictException("La fecha fin debe ser posterior a la fecha inicio");
        }

        Odontologo odontologo = odontologoRepository.findByIdAndActivoTrue(request.odontologoId())
            .orElseThrow(() -> new ResourceNotFoundException("Odontólogo no encontrado"));

        Sede sede = null;
        if (request.sedeId() != null) {
            sede = sedeRepository.findByIdAndActivoTrue(request.sedeId())
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada"));
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        boolean cruce = bloqueoRepository.existeCruceBloqueo(
            request.odontologoId(),
            request.sedeId(),
            request.fechaInicio(),
            request.fechaFin()
        );

        if (cruce) {
            throw new ConflictException("Existe un bloqueo que se cruza con el intervalo indicado");
        }

        BloqueoHorario bloqueo = new BloqueoHorario();
        bloqueo.setOdontologo(odontologo);
        bloqueo.setSede(sede);
        bloqueo.setFechaInicio(request.fechaInicio());
        bloqueo.setFechaFin(request.fechaFin());
        bloqueo.setMotivo(request.motivo());
        bloqueo.setCreadoPor(usuario);

        return mapear(bloqueoRepository.save(bloqueo));
    }

    @Transactional
    public void eliminar(Long id) {
        BloqueoHorario bloqueo = bloqueoRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Bloqueo no encontrado"));

        bloqueoRepository.delete(bloqueo);
    }

    @Transactional(readOnly = true)
    public List<BloqueoHorarioResponse> listar() {
        return bloqueoRepository.findAllByOrderByFechaInicioDesc()
            .stream()
            .map(this::mapear)
            .toList();
    }

    private BloqueoHorarioResponse mapear(BloqueoHorario bloqueo) {
        return new BloqueoHorarioResponse(
            bloqueo.getId(),
            bloqueo.getOdontologo().getId(),
            bloqueo.getSede() != null ? bloqueo.getSede().getId() : null,
            bloqueo.getFechaInicio(),
            bloqueo.getFechaFin(),
            bloqueo.getMotivo(),
            bloqueo.getCreadoPor().getId(),
            bloqueo.getFechaCreacion()
        );
    }
}