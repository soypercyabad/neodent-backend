package com.neodent.notification;

import com.neodent.notification.dto.BienvenidaPersonalEmailData;
import com.neodent.notification.dto.CitaCanceladaEmailData;
import com.neodent.notification.dto.CitaConfirmadaEmailData;
import com.neodent.notification.dto.CitaReprogramadaEmailData;
import com.neodent.shared.constants.AppConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrevoEmailService implements EmailService {

    private final RestClient restClient;
    private final TemplateEngine templateEngine;

    @Value("${brevo.api.url}")
    private String apiUrl;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    @Async("taskExecutor")
    @Override
    public void enviarOtpLogin(String destinatario, String codigo) {
        String html = renderTemplate(AppConstants.PlantillasEmail.LOGIN_OTP, codigo);
        enviar(destinatario, "Código de acceso - NeoDent", html);
    }

    @Async("taskExecutor")
    @Override
    public void enviarVerificacionEmail(String destinatario, String codigo) {
        String html = renderTemplate(AppConstants.PlantillasEmail.EMAIL_VERIFICATION, codigo);
        enviar(destinatario, "Confirma tu correo - NeoDent", html);
    }

    @Async("taskExecutor")
    @Override
    public void enviarOtpActivacionCuenta(String destinatario, String codigo) {
        String html = renderTemplate(AppConstants.PlantillasEmail.ACCOUNT_ACTIVATION_OTP, codigo);
        enviar(destinatario, "Activa tu cuenta - NeoDent", html);
    }

    @Async("taskExecutor")
    @Override
    public void enviarCitaConfirmada(String destinatario, CitaConfirmadaEmailData data) {
        Context context = new Context();
        context.setVariable("nombrePaciente", data.nombrePaciente());
        context.setVariable("fecha", data.fecha());
        context.setVariable("hora", data.hora());
        context.setVariable("odontologo", data.odontologo());
        context.setVariable("especialidad", data.especialidad());
        context.setVariable("sede", data.sede());
        context.setVariable("ctaTexto", data.ctaTexto());
        context.setVariable("ctaUrl", data.ctaUrl());

        String html = templateEngine.process("email/" + AppConstants.PlantillasEmail.CITA_CONFIRMADA, context);
        enviar(destinatario, "Tu cita ha sido programada - NeoDent", html);
    }

    @Async("taskExecutor")
    @Override
    public void enviarRestablecimientoContrasena(String destinatario, String resetUrl) {
        Context context = new Context();
        context.setVariable("resetUrl", resetUrl);

        String html = templateEngine.process(
            "email/" + AppConstants.PlantillasEmail.PASSWORD_RESET,
            context
        );

        enviar(destinatario, "Restablece tu contraseña - NeoDent", html);
    }

    private String renderTemplate(String template, String codigo) {
        Context context = new Context();
        context.setVariable("codigo", codigo);
        return templateEngine.process("email/" + template, context);
    }

    private void enviar(String destinatario, String asunto, String html) {
        Map<String, Object> body = Map.of(
            "sender", Map.of("name", senderName, "email", senderEmail),
            "to", List.of(Map.of("email", destinatario)),
            "subject", asunto,
            "htmlContent", html
        );

        try {
            restClient
                .post()
                .uri(apiUrl + "/v3/smtp/email")
                .header("api-key", apiKey)
                .body(body)
                .retrieve()
                .toBodilessEntity();
            log.info("Correo enviado exitosamente a {} con asunto '{}'", destinatario, asunto);
        } catch (Exception ex) {
            log.error("Error al enviar correo a {} con asunto '{}': {}", destinatario, asunto, ex.getMessage());
        }
    }

    @Async("taskExecutor")
    @Override
    public void enviarCitaReprogramada(String destinatario, CitaReprogramadaEmailData data) {
        Context context = new Context();
        context.setVariable("nombrePaciente", data.nombrePaciente());
        context.setVariable("fechaAnterior", data.fechaAnterior());
        context.setVariable("horaAnterior", data.horaAnterior());
        context.setVariable("fechaNueva", data.fechaNueva());
        context.setVariable("horaNueva", data.horaNueva());
        context.setVariable("odontologo", data.odontologo());
        context.setVariable("especialidad", data.especialidad());
        context.setVariable("sede", data.sede());
        context.setVariable("motivo", data.motivo());
        context.setVariable("ctaUrl", data.ctaUrl());

        String html = templateEngine.process("email/" + AppConstants.PlantillasEmail.CITA_REPROGRAMADA, context);
        enviar(destinatario, "Tu cita fue reprogramada - NeoDent", html);
    }

    @Async("taskExecutor")
    @Override
    public void enviarCitaCancelada(String destinatario, CitaCanceladaEmailData data) {
        Context context = new Context();
        context.setVariable("nombrePaciente", data.nombrePaciente());
        context.setVariable("fecha", data.fecha());
        context.setVariable("hora", data.hora());
        context.setVariable("odontologo", data.odontologo());
        context.setVariable("especialidad", data.especialidad());
        context.setVariable("sede", data.sede());
        context.setVariable("motivo", data.motivo());

        String html = templateEngine.process("email/" + AppConstants.PlantillasEmail.CITA_CANCELADA, context);
        enviar(destinatario, "Tu cita fue cancelada - NeoDent", html);
    }

    @Async("taskExecutor")
    @Override
    public void enviarBienvenidaPersonal(
        String destinatario,
        BienvenidaPersonalEmailData data
    ) {
        Context context = new Context();

        context.setVariable("nombre", data.nombre());
        context.setVariable("correo", data.correo());
        context.setVariable("roles", String.join(", ", data.roles()));
        context.setVariable("loginUrl", data.loginUrl());

        String html = templateEngine.process(
            "email/" + AppConstants.PlantillasEmail.BIENVENIDA_PERSONAL,
            context
        );

        enviar(
            destinatario,
            "Bienvenido(a) a NeoDent",
            html
        );
    }
}