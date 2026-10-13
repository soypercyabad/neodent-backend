package com.neodent.legal.dto;

import java.util.List;

public record TerminosPendientesResponse(
    boolean tienePendiente,
    List<TerminoPendienteDto> terminos
) {}
