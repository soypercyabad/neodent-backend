package com.neodent.dni.util;

import java.util.Set;

/**
 * Utilidad para enmascarar datos personales obtenidos de consultas a servicios externos (RENIEC / Apis.net).
 * Protege la privacidad del usuario en formularios públicos según las normativas de protección de datos personales.
 *
 * Formato configurado:
 * - Primer nombre: completo
 * - Segundo y tercer nombre: primeras 3 letras + ***
 * - Apellido paterno: completo
 * - Apellido materno: primeras 2 letras + **
 * Soporta nombres y apellidos simples y compuestos (partículas 'de', 'del', 'la', 'san', etc.).
 */
public final class DniDataMasker {

    private static final Set<String> PARTICULAS = Set.of(
        "de", "del", "la", "las", "los", "san", "santa"
    );

    private DniDataMasker() {}

    /**
     * Enmascara los nombres:
     * - Primer nombre visible completo.
     * - Nombres posteriores (segundo, tercero, etc.): primeras 3 letras + ***
     * Soporta partículas compuestas como 'de', 'del', 'la'.
     *
     * Ejemplos:
     * - "PERCY ULISES" -> "Percy Uli***"
     * - "JUAN CARLOS ALBERTO" -> "Juan Car*** Alb***"
     * - "MARIA DEL CARMEN" -> "Maria del Car***"
     * - "ANA" -> "Ana"
     */
    public static String enmascararNombres(String nombres) {
        if (nombres == null || nombres.isBlank()) {
            return nombres;
        }

        String[] partes = nombres.trim().split("\\s+");
        if (partes.length == 0) {
            return nombres;
        }

        StringBuilder sb = new StringBuilder();
        // Primer nombre completo
        sb.append(capitalizarPalabra(partes[0]));

        // Nombres posteriores
        for (int i = 1; i < partes.length; i++) {
            String p = partes[i];
            if (p.isBlank()) {
                continue;
            }

            sb.append(" ");
            String pLower = p.toLowerCase();
            if (PARTICULAS.contains(pLower) && i + 1 < partes.length) {
                // Si es partícula intermedia (ej: "del", "de la"), se mantiene
                sb.append(pLower);
            } else {
                // Nombre sustantivo: primeras 3 letras + ***
                sb.append(enmascararConAsteriscos(p, 3, "***"));
            }
        }

        return sb.toString();
    }

    /**
     * Formatea el apellido paterno completo en Capitalizado (Title Case),
     * respetando partículas en apellidos compuestos.
     *
     * Ejemplos:
     * - "MOLINA" -> "Molina"
     * - "DE LA CRUZ" -> "De la Cruz"
     * - "DEL SOLAR" -> "Del Solar"
     * - "SAN MARTIN" -> "San Martin"
     */
    public static String capitalizarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }

        String[] partes = texto.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (p.isBlank()) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append(" ");
            }

            String pLower = p.toLowerCase();
            if (i > 0 && PARTICULAS.contains(pLower)) {
                sb.append(pLower);
            } else {
                sb.append(capitalizarPalabra(p));
            }
        }

        return sb.toString();
    }

    /**
     * Enmascara el apellido materno mostrando las primeras 2 letras + **.
     * Soporta apellidos compuestos manteniendo partículas si aplican.
     *
     * Ejemplos:
     * - "ALVAREZ" -> "Al**"
     * - "QUISPE" -> "Qu**"
     * - "DE LA TORRE" -> "De la To**"
     * - "DEL SOLAR" -> "Del So**"
     */
    public static String enmascararApellidoMaterno(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }

        String[] partes = texto.trim().split("\\s+");
        if (partes.length == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < partes.length; i++) {
            String p = partes[i];
            if (p.isBlank()) {
                continue;
            }

            String pLower = p.toLowerCase();
            boolean esParticula = PARTICULAS.contains(pLower) && i + 1 < partes.length;

            if (sb.length() > 0) {
                sb.append(" ");
            }

            if (esParticula) {
                if (i == 0) {
                    sb.append(capitalizarPalabra(p));
                } else {
                    sb.append(pLower);
                }
            } else {
                // Apellido sustantivo: primeras 2 letras + **
                sb.append(enmascararConAsteriscos(p, 2, "**"));
            }
        }

        return sb.toString();
    }

    /**
     * Compatibilidad con llamadas previas que usaban enmascararInicial.
     */
    public static String enmascararInicial(String texto) {
        return enmascararApellidoMaterno(texto);
    }

    private static String enmascararConAsteriscos(String palabra, int maxLetras, String sufijoAsteriscos) {
        if (palabra == null || palabra.isEmpty()) {
            return "";
        }
        int letrasTomadas = Math.min(maxLetras, palabra.length());
        String prefijo = palabra.substring(0, letrasTomadas);
        return capitalizarPalabra(prefijo) + sufijoAsteriscos;
    }

    private static String capitalizarPalabra(String palabra) {
        if (palabra == null || palabra.isEmpty()) {
            return "";
        }
        if (palabra.length() == 1) {
            return palabra.toUpperCase();
        }
        return Character.toUpperCase(palabra.charAt(0)) + palabra.substring(1).toLowerCase();
    }
}
