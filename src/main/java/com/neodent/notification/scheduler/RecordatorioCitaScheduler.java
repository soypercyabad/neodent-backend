package com.neodent.notification.scheduler;

import com.neodent.cita.model.Cita;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.notification.dto.CitaRecordatorioEmailData;
import com.neodent.notification.model.ConfiguracionNotificacion;
import com.neodent.notification.model.TipoNotificacion;
import com.neodent.notification.service.EmailService;
import com.neodent.notification.service.NotificacionService;
import com.neodent.paciente.model.Paciente;
import com.neodent.personal.model.Personal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioCitaScheduler {

    private static final String CODIGO_CONFIG = "RECORDATORIO_CITA_PROXIMA";
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("hh:mm a");

    private final CitaRepository citaRepository;
    private final NotificacionService notificacionService;
    private final EmailService emailService;
    private final Clock clock;

    @Value("${app.frontend-url:${app.frontend.url:http://localhost:5173}}")
    private String frontendUrl;

    /**
     * Se ejecuta periódicamente para procesar recordatorios de citas próximas.
     * Por defecto cada 15 minutos (900,000 ms), configurable por propiedad.
     */
    @Scheduled(fixedDelayString = "${app.recordatorios.delay:900000}", initialDelay = 15000)
    @Transactional
    public void procesarRecordatoriosProximos() {
        var optConfig = notificacionService.obtenerConfiguracion(CODIGO_CONFIG);
        if (optConfig.isEmpty()) {
            return;
        }

        ConfiguracionNotificacion config = optConfig.get();
        if (!config.isActivo()) {
            return;
        }

        int horasAnticipacion = config.getAnticipacionValor();
        if ("DIAS".equalsIgnoreCase(config.getAnticipacionUnidad())) {
            horasAnticipacion *= 24;
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime limiteSuperior = ahora.plusHours(horasAnticipacion).plusMinutes(1);

        List<Cita> citas = citaRepository.buscarCitasParaRecordatorio(ahora, limiteSuperior);
        if (citas.isEmpty()) {
            return;
        }

        log.info("Procesando recordatorios para {} cita(s) próxima(s) con anticipación de {} horas", citas.size(), horasAnticipacion);

        for (Cita cita : citas) {
            try {
                enviarRecordatorio(cita, config);
                cita.setRecordatorioEnviado(true);
                cita.setRecordatorioEnviadoEn(LocalDateTime.now(clock));
                citaRepository.save(cita);
            } catch (Exception ex) {
                log.error("Error al procesar recordatorio de la cita ID {}: {}", cita.getId(), ex.getMessage(), ex);
            }
        }
    }

    private void enviarRecordatorio(Cita cita, ConfiguracionNotificacion config) {
        Paciente paciente = cita.getPaciente();
        var odontologoEspecialidad = cita.getOdontologoEspecialidad();
        Personal personal = odontologoEspecialidad.getOdontologo().getPersonal();
        String sede = cita.getSede().getNombre();
        String especialidad = odontologoEspecialidad.getEspecialidad().getNombre();

        String nombrePaciente = construirNombre(paciente.getNombres(), paciente.getApellidoPaterno(), paciente.getApellidoMaterno());
        String nombreOdontologo = construirNombre(personal.getNombres(), personal.getApellidoPaterno(), personal.getApellidoMaterno());
        String fechaStr = cita.getFechaHoraInicio().format(FORMATO_FECHA);
        String horaStr = cita.getFechaHoraInicio().format(FORMATO_HORA);

        // 1. Canal Portal (Campanita del usuario)
        if (config.isCanalPortal() && paciente.getUsuario() != null) {
            String mensaje = String.format(
                "Tienes una cita odontológica programada el %s a las %s con el/la Dr(a). %s (%s) en la sede %s.",
                fechaStr, horaStr, nombreOdontologo, especialidad, sede
            );

            notificacionService.crearNotificacion(
                paciente.getUsuario().getId(),
                TipoNotificacion.RECORDATORIO_CITA,
                "Recordatorio de cita próxima",
                mensaje,
                "/mis-citas/" + cita.getId(),
                "CITA",
                cita.getId()
            );
        }

        // 2. Canal Correo Electrónico
        if (config.isCanalCorreo() && paciente.getCorreo() != null && !paciente.getCorreo().isBlank()) {
            CitaRecordatorioEmailData emailData = new CitaRecordatorioEmailData(
                nombrePaciente,
                fechaStr,
                horaStr,
                nombreOdontologo,
                especialidad,
                sede,
                frontendUrl + "/login?redirect=/mis-citas/" + cita.getId()
            );

            emailService.enviarRecordatorioCita(paciente.getCorreo(), emailData);
        }
    }

    private String construirNombre(String nombres, String apePaterno, String apeMaterno) {
        StringBuilder sb = new StringBuilder();
        if (nombres != null) sb.append(nombres.trim());
        if (apePaterno != null) sb.append(" ").append(apePaterno.trim());
        if (apeMaterno != null) sb.append(" ").append(apeMaterno.trim());
        return sb.toString().trim();
    }
}
