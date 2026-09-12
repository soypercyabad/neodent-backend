package com.neodent.auth.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

public record PatientRegistrationRequest(

    @NotBlank
    @Pattern(regexp = "\\d{8}")
    String dni,

    @NotBlank
    String nombres,

    @NotBlank
    String apellidoPaterno,

    String apellidoMaterno,

    LocalDate fechaNacimiento,

    @NotBlank
    @Size(max = 20)
    String telefono,

    @NotBlank
    @Email
    String email,

    String direccion,

    @NotBlank
    @Size(min = 8, max = 100)
    String password,

    @NotBlank
    String turnstileToken

) {}