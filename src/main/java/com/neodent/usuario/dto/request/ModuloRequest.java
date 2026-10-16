package com.neodent.usuario.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ModuloRequest(
    @NotBlank(message = "El código es obligatorio")
    @Size(max = 30, message = "El código no debe superar 30 caracteres")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "El código debe contener solo mayúsculas, números y guiones bajos")
    String codigo,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no debe superar 60 caracteres")
    String nombre,

    @Size(max = 150, message = "La descripción no debe superar 150 caracteres")
    String descripcion,

    @Size(max = 80, message = "La ruta no debe superar 80 caracteres")
    String ruta,

    @NotBlank(message = "El ícono es obligatorio")
    @Size(max = 30, message = "El ícono no debe superar 30 caracteres")
    String icono,

    @NotNull(message = "El orden es obligatorio")
    Integer orden,

    Integer idModuloPadre
) {}
