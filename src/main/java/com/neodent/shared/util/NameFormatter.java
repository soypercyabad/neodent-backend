package com.neodent.shared.util;

public final class NameFormatter {

    private NameFormatter() {}

    public static String format(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String texto = value.trim().toLowerCase().replaceAll("\\s+", " ");
        StringBuilder resultado = new StringBuilder();
        boolean mayuscula = true;

        for (char c : texto.toCharArray()) {
            if (mayuscula && Character.isLetter(c)) {
                resultado.append(Character.toUpperCase(c));
                mayuscula = false;
            } else {
                resultado.append(c);
                mayuscula = (c == ' ' || c == '-' || c == '\'');
            }
        }

        return resultado.toString();
    }
}