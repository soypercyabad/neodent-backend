package com.neodent.notification;

import com.neodent.notification.dto.CitaConfirmadaEmailData;
import com.neodent.shared.constants.AppConstants;
import com.neodent.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BrevoEmailService implements EmailService {

    private final RestClient.Builder restClientBuilder;
    private final TemplateEngine templateEngine;

    @Value("${brevo.api.url}")
    private String apiUrl;

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;


    @Override
    public void enviarOtpLogin(
        String destinatario,
        String codigo
    ) {

        String html = renderTemplate(
            AppConstants.PlantillasEmail.LOGIN_OTP,
            codigo
        );

        enviar(
            destinatario,
            "Código de acceso - NeoDent",
            html
        );
    }


    @Override
    public void enviarVerificacionEmail(
        String destinatario,
        String codigo
    ) {

        String html = renderTemplate(
            AppConstants.PlantillasEmail.EMAIL_VERIFICATION,
            codigo
        );

        enviar(
            destinatario,
            "Confirma tu correo - NeoDent",
            html
        );
    }


    private String renderTemplate(
        String template,
        String codigo
    ) {

        Context context = new Context();

        context.setVariable(
            "codigo",
            codigo
        );

        return templateEngine.process(
            "email/" + template,
            context
        );
    }


    private void enviar(
        String destinatario,
        String asunto,
        String html
    ) {

        Map<String, Object> body = Map.of(
            "sender",
            Map.of(
                "name", senderName,
                "email", senderEmail
            ),

            "to",
            List.of(
                Map.of(
                    "email",
                    destinatario
                )
            ),

            "subject",
            asunto,

            "htmlContent",
            html
        );

        try {

            restClientBuilder
                .baseUrl(apiUrl)
                .build()
                .post()
                .uri("/v3/smtp/email")
                .header(
                    "api-key",
                    apiKey
                )
                .body(body)
                .retrieve()
                .toBodilessEntity();

        } catch (Exception ex) {

            throw new BusinessException(
                "No se pudo enviar el correo electrónico"
            );
        }
    }


    @Override
    public void enviarCitaConfirmada(
        String destinatario,
        CitaConfirmadaEmailData data
    ) {

        Context context = new Context();

        context.setVariable(
            "nombrePaciente",
            data.nombrePaciente()
        );

        context.setVariable(
            "fecha",
            data.fecha()
        );

        context.setVariable(
            "hora",
            data.hora()
        );

        context.setVariable(
            "odontologo",
            data.odontologo()
        );

        context.setVariable(
            "especialidad",
            data.especialidad()
        );

        context.setVariable(
            "sede",
            data.sede()
        );

        context.setVariable(
            "ctaTexto",
            data.ctaTexto()
        );

        context.setVariable(
            "ctaUrl",
            data.ctaUrl()
        );

        String html =
            templateEngine.process(
                "email/" + AppConstants.PlantillasEmail.CITA_CONFIRMADA,
                context
            );

        enviar(
            destinatario,
            "Tu cita ha sido programada - NeoDent",
            html
        );
    }
}