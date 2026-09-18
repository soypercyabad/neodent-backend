package com.neodent.notification.dto;

import java.util.List;

public record BienvenidaPersonalEmailData(
    String nombre,
    String correo,
    List<String> roles,
    String loginUrl
) {}