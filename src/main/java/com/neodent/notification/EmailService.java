package com.neodent.notification;

import com.neodent.notification.dto.BienvenidaPersonalEmailData;
import com.neodent.notification.dto.CitaCanceladaEmailData;
import com.neodent.notification.dto.CitaConfirmadaEmailData;
import com.neodent.notification.dto.CitaReprogramadaEmailData;
import com.neodent.notification.dto.InvitacionCuentaEmailData;

public interface EmailService {

    void enviarOtpLogin(String destinatario, String codigo);

    void enviarVerificacionEmail(String destinatario, String codigo);

    void enviarCitaConfirmada(String destinatario, CitaConfirmadaEmailData data);

    void enviarCitaReprogramada(String destinatario, CitaReprogramadaEmailData data);

    void enviarCitaCancelada(String destinatario, CitaCanceladaEmailData data);

    void enviarOtpActivacionCuenta(String destinatario, String codigo);

    void enviarRestablecimientoContrasena(String destinatario, String resetUrl);

    void enviarBienvenidaPersonal(String destinatario, BienvenidaPersonalEmailData data);

    void enviarInvitacionCuenta(String destinatario, InvitacionCuentaEmailData data);
}