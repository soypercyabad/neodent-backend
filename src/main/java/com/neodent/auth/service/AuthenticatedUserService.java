package com.neodent.auth.service;

import com.neodent.auth.dto.response.AuthenticatedUserResponse;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.personal.model.Personal;
import com.neodent.personal.repository.PersonalRepository;
import com.neodent.shared.exception.UnauthorizedException;
import com.neodent.usuario.dto.response.ModuloResponse;
import com.neodent.usuario.model.Modulo;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {

    private final UsuarioRepository usuarioRepository;
    private final PersonalRepository personalRepository;
    private final PacienteRepository pacienteRepository;

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse obtenerPerfil(Long idUsuario) {

        Usuario usuario = usuarioRepository.findById(idUsuario)
            .orElseThrow(() -> new UnauthorizedException("La cuenta autenticada ya no existe"));

        if (!"ACTIVO".equalsIgnoreCase(usuario.getEstado().getNombre())) {
            throw new UnauthorizedException("La cuenta no se encuentra activa");
        }

        Personal personal = personalRepository.findByUsuarioIdAndActivoTrue(idUsuario).orElse(null);
        Paciente paciente = pacienteRepository.findByUsuarioIdAndActivoTrue(idUsuario).orElse(null);

        String nombres = personal != null ? personal.getNombres() : paciente != null ? paciente.getNombres() : "";
        String apellidoPaterno = personal != null ? personal.getApellidoPaterno() : paciente != null ? paciente.getApellidoPaterno() : "";
        String apellidoMaterno = personal != null ? personal.getApellidoMaterno() : paciente != null ? paciente.getApellidoMaterno() : "";

        List<ModuloResponse> modulos = usuario.getRoles().stream()
            .filter(rol -> Boolean.TRUE.equals(rol.getActivo()))
            .flatMap(rol -> rol.getModulos().stream())
            .filter(modulo -> Boolean.TRUE.equals(modulo.getActivo()))
            .distinct()
            .sorted(Comparator.comparingInt(Modulo::getOrden))
            .map(m -> new ModuloResponse(
                m.getId(),
                m.getCodigo(),
                m.getNombre(),
                m.getDescripcion(),
                m.getRuta(),
                m.getIcono(),
                m.getOrden()
            ))
            .toList();

        return new AuthenticatedUserResponse(
            usuario.getId(),
            usuario.getCorreo(),
            nombres,
            apellidoPaterno,
            apellidoMaterno,
            usuario.getRoles().stream().filter(rol -> Boolean.TRUE.equals(rol.getActivo())).map(rol -> rol.getNombre()).sorted().toList(),
            usuario.getEstado().getNombre(),
            personal != null ? personal.getId() : null,
            paciente != null ? paciente.getId() : null,
            modulos
        );
    }
}