package com.neodent.cita.service;

import com.neodent.auth.service.AccountInvitation;
import com.neodent.auth.service.AccountInvitationService;
import com.neodent.cita.dto.request.CancelarCitaRequest;
import com.neodent.cita.dto.request.MarcarNoAsistioRequest;
import com.neodent.cita.dto.request.ReprogramarCitaRequest;
import com.neodent.cita.dto.response.CitaResponse;
import com.neodent.cita.model.Cita;
import com.neodent.cita.model.EstadoCita;
import com.neodent.cita.model.HistorialCita;
import com.neodent.cita.model.ReservaCitaTemporal;
import com.neodent.cita.repository.BloqueoHorarioRepository;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.cita.repository.EstadoCitaRepository;
import com.neodent.cita.repository.HistorialCitaRepository;
import com.neodent.cita.repository.HorarioOdontologoRepository;
import com.neodent.cita.repository.ReservaCitaTemporalRepository;
import com.neodent.notification.EmailService;
import com.neodent.notification.dto.CitaConfirmadaEmailData;
import com.neodent.paciente.model.Paciente;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.ConflictException;
import com.neodent.shared.exception.ForbiddenException;
import com.neodent.shared.exception.ResourceNotFoundException;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CitaService {

    private final ReservaCitaTemporalRepository reservaRepository;
    private final CitaRepository citaRepository;
    private final EstadoCitaRepository estadoCitaRepository;
    private final BloqueoHorarioRepository bloqueoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AccountInvitationService accountInvitationService;
    private final HistorialCitaRepository historialCitaRepository;
    private final HorarioOdontologoRepository horarioRepository;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Transactional
    public CitaResponse confirmarCita(String tokenReserva, Long usuarioId, String rol) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        ReservaCitaTemporal reserva = reservaRepository.findPorTokenParaConfirmar(tokenReserva)
            .orElseThrow(() -> new ResourceNotFoundException("Reserva temporal no encontrada o ya utilizada"));

        if (AppConstants.Roles.PACIENTE.equals(rol)) {
            Paciente pacienteReserva = reserva.getPaciente();

            if (pacienteReserva.getUsuario() == null || !pacienteReserva.getUsuario().getId().equals(usuarioId)) {
                throw new ForbiddenException("La reserva no pertenece al paciente autenticado");
            }
        }

        if (!reserva.getExpiresAt().isAfter(ahora)) {
            throw new ConflictException("La reserva temporal ha expirado");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        Long odontologoId = reserva.getOdontologoEspecialidad().getOdontologo().getId();

        boolean bloqueado = bloqueoRepository.contarBloqueos(
            odontologoId,
            reserva.getSede().getId(),
            reserva.getFechaHoraInicio(),
            reserva.getFechaHoraFin()
        ) > 0;

        if (bloqueado) {
            throw new ConflictException("El odontólogo ya no está disponible en ese horario");
        }

        boolean existeCita = citaRepository.existeCitaActivaEnHorario(
            odontologoId,
            reserva.getFechaHoraInicio(),
            reserva.getFechaHoraFin()
        );

        if (existeCita) {
            throw new ConflictException("El horario ya se encuentra ocupado");
        }

        EstadoCita estadoProgramada = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.PROGRAMADA)
            .orElseThrow(() -> new ResourceNotFoundException("Estado PROGRAMADA no configurado"));

        Cita cita = new Cita();
        cita.setPaciente(reserva.getPaciente());
        cita.setOdontologoEspecialidad(reserva.getOdontologoEspecialidad());
        cita.setSede(reserva.getSede());
        cita.setServicioSolicitado(reserva.getServicioSolicitado());
        cita.setEstado(estadoProgramada);
        cita.setFechaHoraInicio(reserva.getFechaHoraInicio());
        cita.setFechaHoraFin(reserva.getFechaHoraFin());
        cita.setCreadoPor(usuario);

        Cita citaGuardada = citaRepository.save(cita);

        reserva.setConfirmada(true);
        reservaRepository.save(reserva);

        try {
            enviarCorreoCita(citaGuardada);
        } catch (Exception ex) {
            log.error("La cita {} fue registrada, pero no se pudo enviar el correo: {}", citaGuardada.getId(), ex.getMessage(), ex);
        }

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Cita registrada correctamente"
        );
    }

    private void enviarCorreoCita(Cita cita) {
        Paciente paciente = cita.getPaciente();

        if (paciente.getEmail() == null || paciente.getEmail().isBlank()) {
            log.warn("No se envió correo para la cita {} porque el paciente {} no tiene email", cita.getId(), paciente.getId());
            return;
        }

        String ctaTexto;
        String ctaUrl;

        if (paciente.getUsuario() == null) {
            AccountInvitation invitacion = accountInvitationService.generar(paciente);
            ctaTexto = "CREAR MI CUENTA";
            ctaUrl = invitacion.activationUrl();
        } else {
            ctaTexto = "VER MI PROGRAMACIÓN";
            ctaUrl = frontendUrl + "/login?redirect=/mis-citas";
        }

        var odontologoEspecialidad = cita.getOdontologoEspecialidad();
        var personalOdontologo = odontologoEspecialidad.getOdontologo().getPersonal();

        String nombrePaciente = construirNombre(paciente.getNombres(), paciente.getApellidoPaterno(), paciente.getApellidoMaterno());
        String nombreOdontologo = construirNombre(personalOdontologo.getNombres(), personalOdontologo.getApellidoPaterno(), personalOdontologo.getApellidoMaterno());
        
        DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("hh:mm a");

        CitaConfirmadaEmailData data = new CitaConfirmadaEmailData(
            nombrePaciente,
            cita.getFechaHoraInicio().format(fecha),
            cita.getFechaHoraInicio().format(hora),
            nombreOdontologo,
            odontologoEspecialidad.getEspecialidad().getNombre(),
            cita.getSede().getNombre(),
            ctaTexto,
            ctaUrl
        );

        emailService.enviarCitaConfirmada(paciente.getEmail(), data);
    }

    private String construirNombre(String nombres, String apellidoPaterno, String apellidoMaterno) {
        StringBuilder nombre = new StringBuilder();

        if (nombres != null && !nombres.isBlank()) {
            nombre.append(nombres);
        }
        if (apellidoPaterno != null && !apellidoPaterno.isBlank()) {
            nombre.append(" ").append(apellidoPaterno);
        }
        if (apellidoMaterno != null && !apellidoMaterno.isBlank()) {
            nombre.append(" ").append(apellidoMaterno);
        }

        return nombre.toString().trim();
    }

    @Transactional
    public CitaResponse reprogramarCita(Long citaId, ReprogramarCitaRequest request, Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        Cita cita = citaRepository.findByIdParaActualizar(citaId)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        String estadoActual = cita.getEstado().getNombre();

        if (!AppConstants.EstadosCita.PROGRAMADA.equals(estadoActual) && !AppConstants.EstadosCita.CONFIRMADA.equals(estadoActual)) {
            throw new ConflictException("La cita no puede ser reprogramada en su estado actual");
        }

        LocalDateTime nuevoInicio = request.fechaHoraInicio();

        if (!nuevoInicio.isAfter(ahora)) {
            throw new ConflictException("La nueva fecha y hora debe ser futura");
        }

        long duracion = Duration.between(cita.getFechaHoraInicio(), cita.getFechaHoraFin()).toMinutes();

        if (duracion <= 0) {
            throw new ConflictException("La duración de la cita no es válida");
        }

        LocalDateTime nuevoFin = nuevoInicio.plusMinutes(duracion);
        int diaSemana = nuevoInicio.getDayOfWeek().getValue();

        boolean dentroHorario = horarioRepository.existeHorarioDisponible(
            cita.getOdontologoEspecialidad().getId(),
            cita.getSede().getId(),
            diaSemana,
            nuevoInicio.toLocalTime(),
            nuevoFin.toLocalTime()
        );

        if (!dentroHorario) {
            throw new ConflictException("El horario seleccionado está fuera del horario de atención del odontólogo");
        }

        Long odontologoId = cita.getOdontologoEspecialidad().getOdontologo().getId();

        boolean bloqueado = bloqueoRepository.contarBloqueos(
            odontologoId,
            cita.getSede().getId(),
            nuevoInicio,
            nuevoFin
        ) > 0;

        if (bloqueado) {
            throw new ConflictException("El odontólogo no está disponible en ese horario");
        }

        boolean existeCita = citaRepository.existeOtraCitaActivaEnHorario(citaId, odontologoId, nuevoInicio, nuevoFin);

        if (existeCita) {
            throw new ConflictException("El horario ya se encuentra ocupado");
        }

        boolean existeHold = reservaRepository.existeReservaVigente(odontologoId, nuevoInicio, nuevoFin, ahora);

        if (existeHold) {
            throw new ConflictException("El horario está siendo reservado por otro usuario");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        EstadoCita estadoProgramada = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.PROGRAMADA)
            .orElseThrow(() -> new ResourceNotFoundException("Estado PROGRAMADA no configurado"));

        LocalDateTime fechaAnterior = cita.getFechaHoraInicio();
        EstadoCita estadoAnterior = cita.getEstado();

        cita.setFechaHoraInicio(nuevoInicio);
        cita.setFechaHoraFin(nuevoFin);
        cita.setEstado(estadoProgramada);
        cita.setConfirmadaEn(null);

        Cita citaGuardada = citaRepository.save(cita);

        HistorialCita historial = new HistorialCita();
        historial.setCita(citaGuardada);
        historial.setUsuario(usuario);
        historial.setAccion(AppConstants.AccionesHistorialCita.REPROGRAMADA);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoProgramada);
        historial.setFechaHoraAnterior(fechaAnterior);
        historial.setFechaHoraNueva(nuevoInicio);
        historial.setMotivo(request.motivo());

        historialCitaRepository.save(historial);

        try {
            enviarCorreoCita(citaGuardada);
        } catch (Exception ex) {
            log.error("La cita {} fue reprogramada, pero no se pudo enviar el correo: {}", citaGuardada.getId(), ex.getMessage(), ex);
        }

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Cita reprogramada correctamente"
        );
    }

    @Transactional
    public CitaResponse cancelarCita(Long citaId, CancelarCitaRequest request, Long usuarioId) {
        Cita cita = citaRepository.findByIdParaActualizar(citaId)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        String estadoActual = cita.getEstado().getNombre();

        boolean cancelable = AppConstants.EstadosCita.PROGRAMADA.equals(estadoActual)
            || AppConstants.EstadosCita.CONFIRMADA.equals(estadoActual);

        if (!cancelable) {
            throw new ConflictException("La cita no puede ser cancelada en su estado actual");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        EstadoCita estadoCancelada = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.CANCELADA)
            .orElseThrow(() -> new ResourceNotFoundException("Estado CANCELADA no configurado"));

        EstadoCita estadoAnterior = cita.getEstado();
        LocalDateTime fechaHoraAnterior = cita.getFechaHoraInicio();

        cita.setEstado(estadoCancelada);
        cita.setConfirmadaEn(null);

        Cita citaGuardada = citaRepository.save(cita);

        HistorialCita historial = new HistorialCita();
        historial.setCita(citaGuardada);
        historial.setUsuario(usuario);
        historial.setAccion(AppConstants.AccionesHistorialCita.CANCELADA);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoCancelada);
        historial.setFechaHoraAnterior(fechaHoraAnterior);
        historial.setFechaHoraNueva(null);
        historial.setMotivo(request.motivo());

        historialCitaRepository.save(historial);

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Cita cancelada correctamente"
        );
    }

    @Transactional
    public CitaResponse marcarNoAsistio(Long citaId, MarcarNoAsistioRequest request, Long usuarioId) {
        Cita cita = citaRepository.findByIdParaActualizar(citaId)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        String estadoActual = cita.getEstado().getNombre();

        boolean permitido = AppConstants.EstadosCita.PROGRAMADA.equals(estadoActual)
            || AppConstants.EstadosCita.CONFIRMADA.equals(estadoActual);

        if (!permitido) {
            throw new ConflictException("La cita no puede marcarse como NO_ASISTIO en su estado actual");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        EstadoCita estadoNoAsistio = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.NO_ASISTIO)
            .orElseThrow(() -> new ResourceNotFoundException("Estado NO_ASISTIO no configurado"));

        EstadoCita estadoAnterior = cita.getEstado();
        LocalDateTime fechaHoraAnterior = cita.getFechaHoraInicio();

        cita.setEstado(estadoNoAsistio);
        cita.setConfirmadaEn(null);

        Cita citaGuardada = citaRepository.save(cita);

        HistorialCita historial = new HistorialCita();
        historial.setCita(citaGuardada);
        historial.setUsuario(usuario);
        historial.setAccion(AppConstants.AccionesHistorialCita.NO_ASISTIO);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNoAsistio);
        historial.setFechaHoraAnterior(fechaHoraAnterior);
        historial.setFechaHoraNueva(null);
        historial.setMotivo(request.motivo());

        historialCitaRepository.save(historial);

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Cita marcada como NO_ASISTIO correctamente"
        );
    }

    @Transactional
    public CitaResponse confirmarAsistencia(Long citaId, Long usuarioId) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        Cita cita = citaRepository.findByIdParaActualizar(citaId)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        String estadoActual = cita.getEstado().getNombre();

        if (!AppConstants.EstadosCita.PROGRAMADA.equals(estadoActual)) {
            throw new ConflictException("La cita no puede confirmarse en su estado actual");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        EstadoCita estadoConfirmada = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.CONFIRMADA)
            .orElseThrow(() -> new ResourceNotFoundException("Estado CONFIRMADA no configurado"));

        EstadoCita estadoAnterior = cita.getEstado();

        cita.setEstado(estadoConfirmada);
        cita.setConfirmadaEn(ahora);

        Cita citaGuardada = citaRepository.save(cita);

        HistorialCita historial = new HistorialCita();
        historial.setCita(citaGuardada);
        historial.setUsuario(usuario);
        historial.setAccion(AppConstants.AccionesHistorialCita.CONFIRMADA);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoConfirmada);
        historial.setFechaHoraAnterior(citaGuardada.getFechaHoraInicio());
        historial.setFechaHoraNueva(citaGuardada.getFechaHoraInicio());
        historial.setMotivo("Asistencia confirmada");

        historialCitaRepository.save(historial);

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Asistencia confirmada correctamente"
        );
    }

    @Transactional
    public CitaResponse iniciarAtencion(Long citaId, Long usuarioId) {
        Cita cita = citaRepository.findByIdParaActualizar(citaId)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        validarOdontologoPropietario(cita, usuarioId);

        String estadoActual = cita.getEstado().getNombre();

        if (!AppConstants.EstadosCita.CONFIRMADA.equals(estadoActual)) {
            throw new ConflictException("La cita no puede iniciar atención en su estado actual");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        EstadoCita estadoEnAtencion = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.EN_ATENCION)
            .orElseThrow(() -> new ResourceNotFoundException("Estado EN_ATENCION no configurado"));

        EstadoCita estadoAnterior = cita.getEstado();

        cita.setEstado(estadoEnAtencion);

        Cita citaGuardada = citaRepository.save(cita);

        HistorialCita historial = new HistorialCita();
        historial.setCita(citaGuardada);
        historial.setUsuario(usuario);
        historial.setAccion(AppConstants.AccionesHistorialCita.EN_ATENCION);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoEnAtencion);
        historial.setMotivo("Inicio de atención");

        historialCitaRepository.save(historial);

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Atención iniciada correctamente"
        );
    }

    @Transactional
    public CitaResponse finalizarAtencion(Long citaId, Long usuarioId) {
        Cita cita = citaRepository.findByIdParaActualizar(citaId)
            .orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada"));

        validarOdontologoPropietario(cita, usuarioId);

        String estadoActual = cita.getEstado().getNombre();

        if (!AppConstants.EstadosCita.EN_ATENCION.equals(estadoActual)) {
            throw new ConflictException("La cita no puede finalizar atención en su estado actual");
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario autenticado no encontrado"));

        EstadoCita estadoAtendida = estadoCitaRepository.findByNombreAndActivoTrue(AppConstants.EstadosCita.ATENDIDA)
            .orElseThrow(() -> new ResourceNotFoundException("Estado ATENDIDA no configurado"));

        EstadoCita estadoAnterior = cita.getEstado();

        cita.setEstado(estadoAtendida);

        Cita citaGuardada = citaRepository.save(cita);

        HistorialCita historial = new HistorialCita();
        historial.setCita(citaGuardada);
        historial.setUsuario(usuario);
        historial.setAccion(AppConstants.AccionesHistorialCita.ATENDIDA);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoAtendida);
        historial.setMotivo("Atención finalizada");

        historialCitaRepository.save(historial);

        Integer servicioId = citaGuardada.getServicioSolicitado() != null ? citaGuardada.getServicioSolicitado().getId() : null;

        return new CitaResponse(
            citaGuardada.getId(),
            citaGuardada.getPaciente().getId(),
            citaGuardada.getOdontologoEspecialidad().getId(),
            citaGuardada.getSede().getId(),
            servicioId,
            citaGuardada.getEstado().getNombre(),
            citaGuardada.getFechaHoraInicio(),
            citaGuardada.getFechaHoraFin(),
            "Atención finalizada correctamente"
        );
    }

    private void validarOdontologoPropietario(Cita cita, Long usuarioId) {
        Usuario usuarioOdontologo = cita.getOdontologoEspecialidad().getOdontologo().getPersonal().getUsuario();

        if (usuarioOdontologo == null || !usuarioOdontologo.getId().equals(usuarioId)) {
            throw new ForbiddenException("No tienes permisos para gestionar esta cita");
        }
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> obtenerMisCitas(Long usuarioId) {
        List<Cita> citas = citaRepository.findByPacienteUsuarioIdOrderByFechaHoraInicioDesc(usuarioId);

        return citas.stream()
            .map(cita -> {
                Integer servicioId = cita.getServicioSolicitado() != null ? cita.getServicioSolicitado().getId() : null;

                return new CitaResponse(
                    cita.getId(),
                    cita.getPaciente().getId(),
                    cita.getOdontologoEspecialidad().getId(),
                    cita.getSede().getId(),
                    servicioId,
                    cita.getEstado().getNombre(),
                    cita.getFechaHoraInicio(),
                    cita.getFechaHoraFin(),
                    "Cita consultada correctamente"
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> obtenerMiAgenda(Long usuarioId) {
        List<Cita> citas = citaRepository.findByOdontologoEspecialidadOdontologoPersonalUsuarioIdOrderByFechaHoraInicioDesc(usuarioId);

        return citas.stream()
            .map(cita -> {
                Integer servicioId = cita.getServicioSolicitado() != null ? cita.getServicioSolicitado().getId() : null;

                return new CitaResponse(
                    cita.getId(),
                    cita.getPaciente().getId(),
                    cita.getOdontologoEspecialidad().getId(),
                    cita.getSede().getId(),
                    servicioId,
                    cita.getEstado().getNombre(),
                    cita.getFechaHoraInicio(),
                    cita.getFechaHoraFin(),
                    "Cita consultada correctamente"
                );
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> obtenerAgenda(LocalDate fecha, Long odontologoId, String estado, Integer sedeId) {
        LocalDateTime inicioDia = fecha != null ? fecha.atStartOfDay() : null;
        LocalDateTime finDia = fecha != null ? fecha.plusDays(1).atStartOfDay() : null;

        String estadoNormalizado = estado != null && !estado.isBlank()
            ? estado.trim().toUpperCase()
            : null;

        List<Cita> citas = citaRepository.buscarAgenda(
            odontologoId,
            estadoNormalizado,
            sedeId,
            inicioDia,
            finDia
        );

        return citas.stream()
            .map(cita -> {
                Integer servicioId = cita.getServicioSolicitado() != null ? cita.getServicioSolicitado().getId() : null;

                return new CitaResponse(
                    cita.getId(),
                    cita.getPaciente().getId(),
                    cita.getOdontologoEspecialidad().getId(),
                    cita.getSede().getId(),
                    servicioId,
                    cita.getEstado().getNombre(),
                    cita.getFechaHoraInicio(),
                    cita.getFechaHoraFin(),
                    "Cita consultada correctamente"
                );
            })
            .toList();
    }
}