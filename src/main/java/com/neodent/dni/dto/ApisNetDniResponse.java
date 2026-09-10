package com.neodent.dni.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApisNetDniResponse(

    String numeroDocumento,
    String nombres,
    String apellidoPaterno,
    String apellidoMaterno

) {
}