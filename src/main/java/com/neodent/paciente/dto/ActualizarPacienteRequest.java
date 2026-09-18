package com.neodent.paciente.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ActualizarPacienteRequest(

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 80)
    String nombres,

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 60)
    String apellidoPaterno,

    @Size(max = 60)
    String apellidoMaterno,

    LocalDate fechaNacimiento,

    @Size(max = 20)
    String telefono,

    @Email(message = "El correo electrónico no es válido")
    @Size(max = 120)
    String email,

    @Size(max = 255)
    String direccion
) {
}