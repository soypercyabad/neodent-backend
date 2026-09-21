package com.neodent.usuario.service;

import com.neodent.auth.service.RefreshTokenService;
import com.neodent.auth.service.StaffInvitationService;
import com.neodent.dni.DniService;
import com.neodent.especialidad.model.Especialidad;
import com.neodent.especialidad.model.OdontologoEspecialidad;
import com.neodent.especialidad.repository.EspecialidadRepository;
import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.notification.EmailService;
import com.neodent.notification.dto.BienvenidaPersonalEmailData;
import com.neodent.odontologo.model.Odontologo;
import com.neodent.odontologo.repository.OdontologoRepository;
import com.neodent.paciente.model.TipoDocumento;
import com.neodent.paciente.repository.TipoDocumentoRepository;
import com.neodent.personal.model.Personal;
import com.neodent.personal.repository.PersonalRepository;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.shared.util.NameFormatter;
import com.neodent.usuario.dto.request.ActualizarUsuarioInternoRequest;
import com.neodent.usuario.dto.request.CrearUsuarioInternoRequest;
import com.neodent.usuario.dto.response.UsuarioInternoResponse;
import com.neodent.usuario.dto.response.VerificarDocumentoPersonalResponse;
import com.neodent.usuario.model.EstadoUsuario;
import com.neodent.usuario.model.Rol;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.EstadoUsuarioRepository;
import com.neodent.usuario.repository.RolRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UsuarioInternoService {

    private static final Set<String> ROLES_INTERNOS = Set.of(
        AppConstants.Roles.ADMIN,
        AppConstants.Roles.RECEPCIONISTA,
        AppConstants.Roles.ODONTOLOGO
    );

    private final UsuarioRepository usuarioRepository;
    private final EstadoUsuarioRepository estadoUsuarioRepository;
    private final RolRepository rolRepository;
    private final PersonalRepository personalRepository;
    private final TipoDocumentoRepository tipoDocumentoRepository;
    private final OdontologoRepository odontologoRepository;
    private final EspecialidadRepository especialidadRepository;
    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final DniService dniService;
    private final StaffInvitationService staffInvitationService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public UsuarioInternoResponse crear(CrearUsuarioInternoRequest request) {
        String correo = request.correo().trim().toLowerCase();
        Set<String> nombresRoles = normalizarRoles(request.roles());

        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictException("El correo ya se encuentra registrado");
        }

        if (personalRepository.existsByTipoDocumentoIdAndNumeroDocumentoIgnoreCase(
            request.tipoDocumentoId(),
            request.numeroDocumento().trim()
        )) {
            throw new ConflictException("El documento ya se encuentra registrado");
        }

        TipoDocumento tipoDocumento = tipoDocumentoRepository
            .findByIdAndActivoTrue(request.tipoDocumentoId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no encontrado"));

        EstadoUsuario pendiente = estadoUsuarioRepository
        .findByNombreAndActivoTrue(AppConstants.EstadosUsuario.PENDIENTE)
        .orElseThrow(() -> new ResourceNotFoundException("Estado PENDIENTE no configurado"));

        Usuario usuario = new Usuario();
        usuario.setAliasInterno(generarAlias(correo));
        usuario.setCorreo(correo);
        usuario.setHashContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
        usuario.setEstado(pendiente);
        usuario.setSegundoFactor(true);
        usuario.setCorreoVerificado(false);
        usuario.getRoles().addAll(obtenerRoles(nombresRoles));
        usuario = usuarioRepository.save(usuario);

        Personal personal = new Personal();
        personal.setUsuario(usuario);
        personal.setTipoDocumento(tipoDocumento);
        personal.setNumeroDocumento(request.numeroDocumento().trim());
        personal.setNombres(request.nombres().trim());
        personal.setApellidoPaterno(request.apellidoPaterno().trim());
        personal.setApellidoMaterno(limpiar(request.apellidoMaterno()));
        personal.setTelefono(limpiar(request.telefono()));
        personal.setActivo(true);

        personal = personalRepository.save(personal);

        sincronizarOdontologo(
            personal,
            nombresRoles,
            request.numeroColegiatura(),
            request.especialidadIds()
        );

        BienvenidaPersonalEmailData emailData = new BienvenidaPersonalEmailData(
            construirNombrePersonal(personal),
            usuario.getCorreo(),
            nombresRoles.stream().sorted().toList(),
            staffInvitationService.generar(usuario)
        );

        emailService.enviarBienvenidaPersonal(
            usuario.getCorreo(),
            emailData
        );

        return construirResponse(usuario, personal);
    }

    private String construirNombrePersonal(Personal personal) {
        return String.join(
            " ",
            personal.getNombres(),
            personal.getApellidoPaterno()
        ).trim();
    }

    @Transactional
    public UsuarioInternoResponse actualizar(
        Long usuarioId,
        ActualizarUsuarioInternoRequest request
    ) {
        return actualizar(usuarioId, request, null);
    }

    @Transactional
    public UsuarioInternoResponse actualizar(Long usuarioId, ActualizarUsuarioInternoRequest request, Long usuarioAutenticadoId) {
        Usuario usuario = obtenerUsuario(usuarioId);

        Personal personal = personalRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));

        String correo = request.correo().trim().toLowerCase();
        Set<String> nombresRoles = normalizarRoles(request.roles());

        Set<String> rolesActuales = usuario.getRoles().stream()
            .map(Rol::getNombre)
            .collect(Collectors.toSet());

        boolean eraAdmin = rolesActuales.contains(AppConstants.Roles.ADMIN);
        if (eraAdmin && !nombresRoles.contains(AppConstants.Roles.ADMIN)) {
            if (usuarioAutenticadoId != null && usuarioId.equals(usuarioAutenticadoId)) {
                throw new ConflictException("No puedes remover tu propio rol de administrador");
            }
            if (usuarioRepository.contarAdminsActivos() <= 1) {
                throw new ConflictException("No se puede retirar el rol de administrador al único administrador activo del sistema");
            }
        }

        if (usuarioRepository.existsByCorreoIgnoreCaseAndIdNot(correo, usuarioId)) {
            throw new ConflictException("El correo ya se encuentra registrado");
        }

        if (personalRepository.existsByTipoDocumentoIdAndNumeroDocumentoIgnoreCaseAndIdNot(
            request.tipoDocumentoId(),
            request.numeroDocumento().trim(),
            personal.getId()
        )) {
            throw new ConflictException("El documento ya se encuentra registrado");
        }

        TipoDocumento tipoDocumento = tipoDocumentoRepository
            .findByIdAndActivoTrue(request.tipoDocumentoId())
            .orElseThrow(() -> new ResourceNotFoundException("Tipo de documento no encontrado"));

        usuario.setCorreo(correo);

        boolean contrasenaCambio = request.nuevaContrasena() != null && !request.nuevaContrasena().isBlank();
        boolean rolesCambiaron = !rolesActuales.equals(nombresRoles);

        if (contrasenaCambio) {
            if (request.nuevaContrasena().trim().length() < 8) {
                throw new ConflictException("La nueva contraseña debe tener al menos 8 caracteres");
            }
            usuario.setHashContrasena(
                passwordEncoder.encode(request.nuevaContrasena())
            );
        }

        if (contrasenaCambio || rolesCambiaron) {
            refreshTokenService.revocarTodos(usuario.getId());
        }

        usuario.getRoles().clear();
        usuario.getRoles().addAll(obtenerRoles(nombresRoles));

        personal.setTipoDocumento(tipoDocumento);
        personal.setNumeroDocumento(request.numeroDocumento().trim());
        personal.setNombres(NameFormatter.format(request.nombres()));
        personal.setApellidoPaterno(NameFormatter.format(request.apellidoPaterno()));
        personal.setApellidoMaterno(NameFormatter.format(request.apellidoMaterno()));
        personal.setTelefono(limpiar(request.telefono()));

        usuarioRepository.save(usuario);
        personalRepository.save(personal);

        sincronizarOdontologo(
            personal,
            nombresRoles,
            request.numeroColegiatura(),
            request.especialidadIds()
        );

        return construirResponse(usuario, personal);
    }

    @Transactional(readOnly = true)
    public Page<UsuarioInternoResponse> listar(
        String rol,
        String estado,
        Boolean activo,
        String buscar,
        Pageable pageable
    ) {
        String rolNormalizado = normalizarFiltro(rol);
        String estadoNormalizado = normalizarFiltro(estado);
        String buscarNormalizado =
            buscar == null || buscar.isBlank() ? null : buscar.trim();

        if (rolNormalizado != null && !ROLES_INTERNOS.contains(rolNormalizado)) {
            throw new ConflictException("Rol interno no válido");
        }

        Page<Usuario> usuariosPage = usuarioRepository.buscarUsuariosInternos(
            rolNormalizado,
            estadoNormalizado,
            activo,
            buscarNormalizado,
            pageable
        );

        if (usuariosPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<Usuario> usuarios = usuariosPage.getContent();
        List<Long> usuarioIds = usuarios.stream().map(Usuario::getId).toList();

        List<Personal> personalList = personalRepository.findAllByUsuarioIdInWithTipoDocumento(usuarioIds);
        Map<Long, Personal> personalPorUsuarioId = personalList.stream()
            .collect(Collectors.toMap(p -> p.getUsuario().getId(), Function.identity()));

        List<Long> personalIds = personalList.stream().map(Personal::getId).toList();
        List<Odontologo> odontologos = personalIds.isEmpty()
            ? List.of()
            : odontologoRepository.findAllByPersonalIdIn(personalIds);

        Map<Long, Odontologo> odontologoPorPersonalId = odontologos.stream()
            .collect(Collectors.toMap(o -> o.getPersonal().getId(), Function.identity()));

        List<Long> odontologoIds = odontologos.stream().map(Odontologo::getId).toList();
        Map<Long, List<OdontologoEspecialidad>> especialidadesPorOdontologoId = odontologoIds.isEmpty()
            ? Map.of()
            : odontologoEspecialidadRepository
                .findAllByOdontologoIdInAndActivoTrueWithEspecialidad(odontologoIds)
                .stream()
                .collect(Collectors.groupingBy(oe -> oe.getOdontologo().getId()));

        return usuariosPage.map(usuario -> {
            Personal personal = personalPorUsuarioId.get(usuario.getId());
            if (personal == null) {
                throw new ResourceNotFoundException(
                    "El usuario interno no tiene registro PERSONAL"
                );
            }

            Odontologo odontologo = odontologoPorPersonalId.get(personal.getId());
            List<OdontologoEspecialidad> relaciones = odontologo != null
                ? especialidadesPorOdontologoId.getOrDefault(odontologo.getId(), List.of())
                : List.of();

            List<Integer> especialidadIds = relaciones.stream()
                .map(r -> r.getEspecialidad().getId())
                .sorted()
                .toList();

            List<String> especialidades = relaciones.stream()
                .map(r -> r.getEspecialidad().getNombre())
                .sorted()
                .toList();

            return new UsuarioInternoResponse(
                usuario.getId(),
                usuario.getAliasInterno(),
                usuario.getCorreo(),
                usuario.getEstado().getNombre(),
                usuario.getRoles().stream()
                    .map(Rol::getNombre)
                    .sorted()
                    .toList(),

                personal.getId(),
                personal.getTipoDocumento().getId(),
                personal.getNumeroDocumento(),
                personal.getNombres(),
                personal.getApellidoPaterno(),
                personal.getApellidoMaterno(),
                personal.getTelefono(),
                personal.getActivo(),

                odontologo != null ? odontologo.getId() : null,
                odontologo != null ? odontologo.getNumeroColegiatura() : null,
                especialidadIds,
                especialidades
            );
        });
    }

    private String normalizarFiltro(String valor) {
        return valor == null || valor.isBlank()
            ? null
            : valor.trim().toUpperCase();
    }

    @Transactional(readOnly = true)
    public UsuarioInternoResponse obtener(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);

        Personal personal = personalRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));

        return construirResponse(usuario, personal);
    }

    @Transactional
    public UsuarioInternoResponse desactivar(Long usuarioId) {
        return desactivar(usuarioId, null);
    }

    @Transactional
    public UsuarioInternoResponse desactivar(Long usuarioId, Long usuarioAutenticadoId) {
        if (usuarioAutenticadoId != null && usuarioId.equals(usuarioAutenticadoId)) {
            throw new ConflictException("No puedes desactivar tu propia cuenta de administrador");
        }

        Usuario usuario = obtenerUsuario(usuarioId);

        boolean esAdmin = usuario.getRoles().stream()
            .anyMatch(r -> AppConstants.Roles.ADMIN.equals(r.getNombre()));

        if (esAdmin && usuarioRepository.contarAdminsActivos() <= 1) {
            throw new ConflictException("No se puede desactivar al único administrador activo del sistema");
        }

        Personal personal = personalRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));

        EstadoUsuario inactivo = estadoUsuarioRepository
            .findByNombreAndActivoTrue(AppConstants.EstadosUsuario.INACTIVO)
            .orElseThrow(() -> new ResourceNotFoundException("Estado INACTIVO no configurado"));

        usuario.setEstado(inactivo);
        personal.setActivo(false);

        odontologoRepository.findByPersonalId(personal.getId())
            .ifPresent(odontologo -> odontologo.setActivo(false));

        refreshTokenService.revocarTodos(usuarioId);

        return construirResponse(usuario, personal);
    }

    @Transactional
    public UsuarioInternoResponse activar(Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);

        if (AppConstants.EstadosUsuario.PENDIENTE.equals(usuario.getEstado().getNombre()) || !Boolean.TRUE.equals(usuario.getCorreoVerificado())) {
            throw new ConflictException("El trabajador debe completar la invitación y crear su contraseña antes de activar su cuenta");
        }

        Personal personal = personalRepository.findByUsuarioId(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Personal no encontrado"));

        EstadoUsuario activo = estadoUsuarioRepository
            .findByNombreAndActivoTrue(AppConstants.EstadosUsuario.ACTIVO)
            .orElseThrow(() -> new ResourceNotFoundException("Estado ACTIVO no configurado"));

        usuario.setEstado(activo);
        personal.setActivo(true);

        boolean esOdontologo = usuario.getRoles().stream()
            .anyMatch(r -> AppConstants.Roles.ODONTOLOGO.equals(r.getNombre()));

        if (esOdontologo) {
            odontologoRepository.findByPersonalId(personal.getId())
                .ifPresent(odontologo -> odontologo.setActivo(true));
        }

        return construirResponse(usuario, personal);
    }

    private void sincronizarOdontologo(
        Personal personal,
        Set<String> roles,
        String numeroColegiatura,
        Set<Integer> especialidadIds
    ) {
        boolean requiereOdontologo = roles.contains(AppConstants.Roles.ODONTOLOGO);

        Optional<Odontologo> existente =
            odontologoRepository.findByPersonalId(personal.getId());

        if (!requiereOdontologo) {
            existente.ifPresent(odontologo -> {
                odontologo.setActivo(false);

                odontologoEspecialidadRepository
                    .findAllByOdontologoId(odontologo.getId())
                    .forEach(relacion -> relacion.setActivo(false));
            });
            return;
        }

        if (numeroColegiatura == null || numeroColegiatura.isBlank()) {
            throw new ConflictException(
                "El número de colegiatura es obligatorio para un odontólogo"
            );
        }

        if (especialidadIds == null || especialidadIds.isEmpty()) {
            throw new ConflictException(
                "Debe seleccionar al menos una especialidad"
            );
        }

        Odontologo odontologo = existente.orElseGet(Odontologo::new);

        if (odontologo.getId() == null) {
            if (odontologoRepository.existsByNumeroColegiaturaIgnoreCase(numeroColegiatura)) {
                throw new ConflictException("La colegiatura ya se encuentra registrada");
            }
            odontologo.setPersonal(personal);
        } else if (odontologoRepository.existsByNumeroColegiaturaIgnoreCaseAndIdNot(
            numeroColegiatura,
            odontologo.getId()
        )) {
            throw new ConflictException("La colegiatura ya se encuentra registrada");
        }

        odontologo.setNumeroColegiatura(numeroColegiatura.trim());
        odontologo.setActivo(true);
        odontologo = odontologoRepository.save(odontologo);

        List<OdontologoEspecialidad> actuales =
            odontologoEspecialidadRepository.findAllByOdontologoId(odontologo.getId());

        for (OdontologoEspecialidad relacion : actuales) {
            relacion.setActivo(
                especialidadIds.contains(relacion.getEspecialidad().getId())
            );
        }

        for (Integer especialidadId : especialidadIds) {
            Especialidad especialidad = especialidadRepository
                .findByIdAndActivoTrue(especialidadId)
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Especialidad no encontrada: " + especialidadId
                ));

            Optional<OdontologoEspecialidad> relacion =
                odontologoEspecialidadRepository
                    .findByOdontologoIdAndEspecialidadId(
                        odontologo.getId(),
                        especialidadId
                    );

            if (relacion.isPresent()) {
                relacion.get().setActivo(true);
            } else {
                OdontologoEspecialidad nueva = new OdontologoEspecialidad();
                nueva.setOdontologo(odontologo);
                nueva.setEspecialidad(especialidad);
                nueva.setActivo(true);
                odontologoEspecialidadRepository.save(nueva);
            }
        }
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findConRolesById(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Set<String> normalizarRoles(Set<String> roles) {
        Set<String> normalizados = new HashSet<>();

        for (String rol : roles) {
            String nombre = rol.trim().toUpperCase();

            if (!ROLES_INTERNOS.contains(nombre)) {
                throw new ConflictException(
                    "Rol interno no permitido: " + nombre
                );
            }

            normalizados.add(nombre);
        }

        return normalizados;
    }

    private Set<Rol> obtenerRoles(Set<String> roles) {
        Set<Rol> resultado = new HashSet<>();

        for (String nombre : roles) {
            Rol rol = rolRepository.findByNombreAndActivoTrue(nombre)
                .orElseThrow(() ->
                    new ResourceNotFoundException(
                        "Rol no configurado: " + nombre
                    ));

            resultado.add(rol);
        }

        return resultado;
    }

    private String generarAlias(String correo) {
        String base = correo.substring(0, correo.indexOf("@"))
            .toLowerCase()
            .replaceAll("[^a-z0-9._-]", "");

        if (base.isBlank()) {
            base = "usuario";
        }

        if (base.length() > 45) {
            base = base.substring(0, 45);
        }

        String alias = base;
        int contador = 2;

        while (usuarioRepository.existsByAliasInternoIgnoreCase(alias)) {
            alias = base + contador++;
        }

        return alias;
    }

    private String limpiar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private UsuarioInternoResponse construirResponse(Usuario usuario, Personal personal) {
        if (personal == null) {
            throw new ResourceNotFoundException(
                "El usuario interno no tiene registro PERSONAL"
            );
        }

        Optional<Odontologo> odontologo =
            odontologoRepository.findByPersonalId(personal.getId());

        List<OdontologoEspecialidad> relaciones = odontologo
            .map(o -> odontologoEspecialidadRepository
                .findAllByOdontologoIdAndActivoTrueWithEspecialidad(o.getId()))
            .orElseGet(List::of);

        List<Integer> especialidadIds = relaciones.stream()
            .map(r -> r.getEspecialidad().getId())
            .sorted()
            .toList();

        List<String> especialidades = relaciones.stream()
            .map(r -> r.getEspecialidad().getNombre())
            .sorted()
            .toList();

        return new UsuarioInternoResponse(
            usuario.getId(),
            usuario.getAliasInterno(),
            usuario.getCorreo(),
            usuario.getEstado().getNombre(),
            usuario.getRoles().stream()
                .map(Rol::getNombre)
                .sorted()
                .toList(),

            personal.getId(),
            personal.getTipoDocumento().getId(),
            personal.getNumeroDocumento(),
            personal.getNombres(),
            personal.getApellidoPaterno(),
            personal.getApellidoMaterno(),
            personal.getTelefono(),
            personal.getActivo(),

            odontologo.map(Odontologo::getId).orElse(null),
            odontologo.map(Odontologo::getNumeroColegiatura).orElse(null),
            especialidadIds,
            especialidades
        );
    }

    @Transactional(readOnly = true)
    public VerificarDocumentoPersonalResponse verificarDocumento(
        Integer tipoDocumentoId,
        String numeroDocumento
    ) {
        String documento = numeroDocumento.trim();

        if (personalRepository
            .existsByTipoDocumentoIdAndNumeroDocumentoIgnoreCase(
                tipoDocumentoId,
                documento
            )) {
            throw new ConflictException(
                "El documento ya se encuentra registrado como personal"
            );
        }

        TipoDocumento tipoDocumento = tipoDocumentoRepository
            .findByIdAndActivoTrue(tipoDocumentoId)
            .orElseThrow(() ->
                new ResourceNotFoundException(
                    "Tipo de documento no encontrado"
                )
            );

        if (!"DNI".equalsIgnoreCase(tipoDocumento.getCodigo())) {
            return new VerificarDocumentoPersonalResponse(
                true,
                false,
                true,
                null,
                null,
                null,
                "Documento disponible. Complete los datos manualmente"
            );
        }

        try {
            var datos = dniService.buscarPorDni(documento);

            if (datos == null) {
                return respuestaManual();
            }

            return new VerificarDocumentoPersonalResponse(
                true,
                true,
                false,
                datos.nombres(),
                datos.apellidoPaterno(),
                datos.apellidoMaterno(),
                "Documento disponible y datos encontrados"
            );

        } catch (Exception ex) {
            return respuestaManual();
        }
    }

    private VerificarDocumentoPersonalResponse respuestaManual() {
        return new VerificarDocumentoPersonalResponse(
            true,
            false,
            true,
            null,
            null,
            null,
            "No se pudieron obtener los datos. Complete el formulario manualmente"
        );
    }
}