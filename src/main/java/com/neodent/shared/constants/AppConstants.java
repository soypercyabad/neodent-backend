package com.neodent.shared.constants;

public final class AppConstants {

    private AppConstants() {}

    public static final class Roles {
        public static final String ADMIN = "ADMIN";
        public static final String RECEPCIONISTA = "RECEPCIONISTA";
        public static final String ODONTOLOGO = "ODONTOLOGO";
        public static final String PACIENTE = "PACIENTE";
        public static final String PREFIX = "ROLE_";

        private Roles() {}
    }

    public static final class EstadosUsuario {
        public static final String ACTIVO = "ACTIVO";
        public static final String INACTIVO = "INACTIVO";
        public static final String PENDIENTE = "PENDIENTE";
        public static final String BLOQUEADO = "BLOQUEADO";

        private EstadosUsuario() {}
    }

    public static final class TiposDocumento {
        public static final String DNI = "DNI";
        public static final String CE = "CE";
        public static final String PASAPORTE = "PASAPORTE";

        private TiposDocumento() {}
    }

    public static final class TiposOtp {
        public static final String LOGIN_2FA = "LOGIN_2FA";
        public static final String EMAIL_VERIFICATION = "EMAIL_VERIFICATION";
        public static final String ACCOUNT_ACTIVATION = "ACCOUNT_ACTIVATION";

        private TiposOtp() {}
    }

    public static final class TiposTokenAccion {
        public static final String ACCOUNT_INVITATION = "ACCOUNT_INVITATION";
        public static final String PASSWORD_RESET = "PASSWORD_RESET";

        private TiposTokenAccion() {}
    }

    public static final class PlantillasEmail {
        public static final String LOGIN_OTP = "login-otp";
        public static final String EMAIL_VERIFICATION = "email-verification";
        public static final String CITA_CONFIRMADA = "cita-confirmada";
        public static final String PASSWORD_RESET = "password-reset";
        public static final String ACCOUNT_ACTIVATION_OTP = "account-activation-otp";

        private PlantillasEmail() {}
    }
}
