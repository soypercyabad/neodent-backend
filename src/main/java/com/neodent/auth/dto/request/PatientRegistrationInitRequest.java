package com.neodent.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientRegistrationInitRequest(

    @NotBlank(message = "El tipo de documento es obligatorio")
    @Size(max = 10, message = "El tipo de documento no es válido")
    String tipoDocumento,

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20, message = "El número de documento no puede superar los 20 caracteres")
    String numeroDocumento,

    @NotBlank(message = "Los nombres son obligatorios")
    String nombres,

    @NotBlank(message = "El apellido paterno es obligatorio")
    String apellidoPaterno,

    String apellidoMaterno,

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    LocalDate fechaNacimiento,

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\+?\\d{7,15}$",message = "El teléfono no tiene un formato válido")
    String telefono,

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 120, message = "El correo electrónico no puede superar los 120 caracteres")
    String email,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s]).+$",
        message = "La contraseña debe incluir al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    String password,

    @NotNull(message = "Debes indicar la aceptación de los términos")
    Boolean aceptaTerminos,

    @NotBlank(message = "La verificación de seguridad es obligatoria")
    String turnstileToken
) {}