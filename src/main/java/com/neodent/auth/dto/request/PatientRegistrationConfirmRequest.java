package com.neodent.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PatientRegistrationConfirmRequest(
    @NotNull(message = "El challengeId es obligatorio")
    Long challengeId,

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El código debe contener 6 dígitos")
    String codigo,

    @NotBlank @Size(max = 10)
    String tipoDocumento,

    @NotBlank @Size(max = 20)
    String numeroDocumento,

    @NotBlank
    String nombres,

    @NotBlank
    String apellidoPaterno,

    String apellidoMaterno,

    LocalDate fechaNacimiento,

    @NotBlank @Size(max = 20)
    String telefono,

    @NotBlank @Email
    String email,

    String direccion,

    @NotBlank @Size(min = 8, max = 100)
    String password
) {}
