package com.neodent.auth.dto.request;

import jakarta.validation.constraints.*;

public record PatientDocumentCheckRequest(
    @NotBlank @Size(max = 10) 
    String tipoDocumento,
    
    @NotBlank @Size(max = 20) 
    String numeroDocumento,
    
    @NotBlank 
    String turnstileToken
) {}