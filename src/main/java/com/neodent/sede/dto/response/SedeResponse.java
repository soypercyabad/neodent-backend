package com.neodent.sede.dto.response;

import java.time.LocalDateTime;

public record SedeResponse(
    Integer id,
    String nombre,
    String direccion,
    String distrito,
    String provincia,
    String departamento,
    String telefono,
    String email,
    Boolean activo,
    LocalDateTime fechaCreacion
) {}