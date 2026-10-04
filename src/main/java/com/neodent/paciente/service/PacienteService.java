package com.neodent.paciente.service;

import com.neodent.paciente.dto.ActualizarPacienteRequest;
import com.neodent.paciente.dto.CrearPacienteRequest;
import com.neodent.paciente.dto.PacienteResponse;
import com.neodent.paciente.mapper.PacienteMapper;
import com.neodent.paciente.model.Paciente;
import com.neodent.paciente.model.TipoDocumento;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.paciente.repository.TipoDocumentoRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.shared.util.NameFormatter;
import com.neodent.usuario.model.EstadoUsuario;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.EstadoUsuarioRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final PacienteMapper pacienteMapper;
    private final UsuarioRepository usuarioRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorDocumento(String tipoDocumento, String numeroDocumento) {
        Paciente paciente = pacienteRepository
            .findByTipoDocumentoCodigoAndNumeroDocumento(tipoDocumento.trim().toUpperCase(), numeroDocumento.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));

        return pacienteMapper.toResponse(paciente);
    }

    @Transactional(readOnly = true)
    public PacienteResponse buscarPorId(Long id) {
        Paciente paciente = obtenerPacienteOFail(id);
        return pacienteMapper.toResponse(paciente);
    }

    @Transactional
    public PacienteResponse crear(CrearPacienteRequest request) {
        String codigoDocumento = request.tipoDocumento().trim().toUpperCase();
        String numeroDocumento = request.numeroDocumento().trim();

        TipoDocumento tipoDocumento = tipoDocumentoRepository.findByCodigoAndActivoTrue(codigoDocumento)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no válido"));

        if (pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(codigoDocumento, numeroDocumento)) {
            throw new ConflictException("Ya existe un paciente registrado con este documento");
        }

        String telefono = normalizarTextoOpcional(request.telefono());
        String email = normalizarEmail(request.email());
        validarDatosUnicos(email, telefono, null, null);

        Paciente paciente = new Paciente();
        paciente.setTipoDocumento(tipoDocumento);
        paciente.setNumeroDocumento(numeroDocumento);
        paciente.setNombres(NameFormatter.format(request.nombres()));
        paciente.setApellidoPaterno(NameFormatter.format(request.apellidoPaterno()));
        paciente.setApellidoMaterno(NameFormatter.format(request.apellidoMaterno()));
        paciente.setFechaNacimiento(request.fechaNacimiento());
        paciente.setTelefono(telefono);
        paciente.setCorreo(email);
        paciente.setDireccion(normalizarTextoOpcional(request.direccion()));
        paciente.setUsuario(null);
        paciente.setActivo(true);

        Paciente guardado = pacienteRepository.save(paciente);
        return pacienteMapper.toResponse(guardado);
    }

    @Transactional
    public PacienteResponse actualizar(Long id, ActualizarPacienteRequest request) {
        Paciente paciente = obtenerPacienteOFail(id);

        if (!paciente.getActivo()) {
            throw new ConflictException("No se puede modificar un paciente inactivo");
        }

        String telefono = normalizarTextoOpcional(request.telefono());
        String email = normalizarEmail(request.email());

        Usuario usuario = paciente.getUsuario();
        Long usuarioId = usuario != null ? usuario.getId() : null;

        if (usuario != null && email == null) {
            throw new ConflictException("Un paciente con cuenta de usuario no puede quedar sin correo electrónico");
        }

        validarDatosUnicos(email, telefono, id, usuarioId);

        String emailAnterior = paciente.getCorreo();
        boolean cambioEmail = (email != null && !email.equalsIgnoreCase(emailAnterior))
                || (email == null && emailAnterior != null);

        paciente.setNombres(NameFormatter.format(request.nombres()));
        paciente.setApellidoPaterno(NameFormatter.format(request.apellidoPaterno()));
        paciente.setApellidoMaterno(NameFormatter.format(request.apellidoMaterno()));
        paciente.setFechaNacimiento(request.fechaNacimiento());
        paciente.setTelefono(telefono);
        paciente.setCorreo(email);
        paciente.setDireccion(normalizarTextoOpcional(request.direccion()));

        if (usuario != null && cambioEmail) {
            usuario.setCorreo(email);
            usuario.setCorreoVerificado(false);

            EstadoUsuario estadoPendiente = estadoUsuarioRepository
                    .findByNombreAndActivoTrue(AppConstants.EstadosUsuario.PENDIENTE)
                    .orElseThrow(() -> new IllegalStateException("Estado PENDIENTE no configurado"));
            usuario.setEstado(estadoPendiente);
            usuarioRepository.save(usuario);
        }

        Paciente actualizado = pacienteRepository.save(paciente);
        return pacienteMapper.toResponse(actualizado);
    }

    @Transactional
    public void activar(Long id) {
        Paciente paciente = obtenerPacienteOFail(id);

        if (paciente.getActivo()) {
            throw new ConflictException("El paciente ya se encuentra activo");
        }

        paciente.setActivo(true);
        pacienteRepository.save(paciente);
    }

    @Transactional
    public void desactivar(Long id) {
        Paciente paciente = obtenerPacienteOFail(id);

        if (!paciente.getActivo()) {
            throw new ConflictException("El paciente ya se encuentra inactivo");
        }

        paciente.setActivo(false);
        pacienteRepository.save(paciente);
    }

    @Transactional
    public Paciente crearConUsuario(CrearPacienteRequest request, Usuario usuario) {
        String codigoDocumento = request.tipoDocumento().trim().toUpperCase();
        String numeroDocumento = request.numeroDocumento().trim();

        TipoDocumento tipoDocumento = tipoDocumentoRepository.findByCodigoAndActivoTrue(codigoDocumento)
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no válido"));

        if (pacienteRepository.existsByTipoDocumentoCodigoAndNumeroDocumento(codigoDocumento, numeroDocumento)) {
            throw new ConflictException("Ya existe un paciente registrado con este documento");
        }

        String telefono = normalizarTextoOpcional(request.telefono());
        String email = normalizarEmail(request.email());
        validarDatosUnicos(email, telefono, null, usuario != null ? usuario.getId() : null);

        Paciente paciente = new Paciente();
        paciente.setTipoDocumento(tipoDocumento);
        paciente.setNumeroDocumento(numeroDocumento);
        paciente.setNombres(NameFormatter.format(request.nombres()));
        paciente.setApellidoPaterno(NameFormatter.format(request.apellidoPaterno()));
        paciente.setApellidoMaterno(NameFormatter.format(request.apellidoMaterno()));
        paciente.setFechaNacimiento(request.fechaNacimiento());
        paciente.setTelefono(telefono);
        paciente.setCorreo(email);
        paciente.setDireccion(normalizarTextoOpcional(request.direccion()));
        paciente.setUsuario(usuario);
        paciente.setActivo(true);

        return pacienteRepository.save(paciente);
    }

    private Paciente obtenerPacienteOFail(Long id) {
        return pacienteRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado"));
    }

    private void validarDatosUnicos(String email, String telefono, Long pacienteId, Long usuarioId) {
        if (email != null) {
            boolean existeEmailPaciente = pacienteId == null
                ? pacienteRepository.existsByCorreoIgnoreCase(email)
                : pacienteRepository.existsByCorreoIgnoreCaseAndIdNot(email, pacienteId);

            if (existeEmailPaciente) {
                throw new ConflictException("Ya existe un paciente registrado con este correo electrónico");
            }

            boolean existeEmailUsuario = usuarioId == null
                ? usuarioRepository.existsByCorreoIgnoreCase(email)
                : usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(email, usuarioId);

            if (existeEmailUsuario) {
                throw new ConflictException("Ya existe una cuenta de usuario registrada con este correo electrónico");
            }
        }

        if (telefono != null) {
            boolean existeTelefono = pacienteId == null
                ? pacienteRepository.existsByTelefono(telefono)
                : pacienteRepository.existsByTelefonoAndIdNot(telefono, pacienteId);

            if (existeTelefono) {
                throw new ConflictException("Ya existe un paciente registrado con este número de teléfono");
            }
        }
    }

    private String normalizarTextoOpcional(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }

    @Transactional(readOnly = true)
    public Page<PacienteResponse> listar(
        Boolean activo,
        Boolean conCuenta,
        String buscar,
        Pageable pageable
    ) {
        String buscarNormalizado =
            buscar == null || buscar.isBlank()
                ? null
                : buscar.trim();

        return pacienteRepository.buscarPacientes(
            activo,
            conCuenta,
            buscarNormalizado,
            pageable
        ).map(pacienteMapper::toResponse);
    }
}
