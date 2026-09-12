package com.neodent.notification;

import com.neodent.notification.dto.CitaConfirmadaEmailData;

public interface EmailService {

    void enviarOtpLogin(
        String destinatario,
        String codigo
    );

    void enviarVerificacionEmail(
        String destinatario,
        String codigo
    );

    void enviarCitaConfirmada(
        String destinatario,
        CitaConfirmadaEmailData data
    );

    void enviarOtpActivacionCuenta(
        String destinatario,
        String codigo
    );
}