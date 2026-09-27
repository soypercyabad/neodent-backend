package com.neodent.servicio.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ServicioResponse(
    Integer id,
    Integer especialidadId,
    String especialidadNombre,
    String nombre,
    String descripcion,
    Integer duracionMinutos,
    BigDecimal precioReferencial,
    Boolean activo,
    List<Integer> sedeIds
) {}