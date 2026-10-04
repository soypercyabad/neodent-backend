package com.neodent.servicio.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record ServicioRequest(

    @NotNull(message = "Selecciona una especialidad")
    Integer especialidadId,

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    String nombre,

    @Size(max = 255)
    String descripcion,

    @NotNull(message = "La duración es obligatoria")
    @Min(1) @Max(1440)
    Integer duracionMinutos,

    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    BigDecimal precioReferencial,

    Boolean destacado,

    @NotNull(message = "Debes indicar las sedes del servicio")
    List<Integer> sedeIds

) {}