package com.neodent.cita.dto.request;

import jakarta.validation.constraints.Size;

public record MarcarNoAsistioRequest(

    @Size(max = 255)
    String motivo

) {}