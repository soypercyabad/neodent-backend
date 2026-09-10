package com.neodent.paciente.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(
    name = "CrearPacienteRequest",
    description = "Datos requeridos para registrar un paciente"
)
public record CrearPacienteRequest(

    @Schema(
        description = "Código del tipo de documento",
        example = "DNI"
    )
    @NotBlank(message = "El tipo de documento es obligatorio")
    String tipoDocumento,

    @Schema(
        description = "Número de documento",
        example = "74567891"
    )
    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 20)
    String numeroDocumento,

    @Schema(
        description = "Nombres del paciente",
        example = "Lucia Maria"
    )
    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 80)
    String nombres,

    @Schema(
        description = "Apellido paterno",
        example = "Mendoza"
    )
    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(max = 60)
    String apellidoPaterno,

    @Schema(
        description = "Apellido materno",
        example = "Silva"
    )
    @Size(max = 60)
    String apellidoMaterno,

    @Schema(
        description = "Fecha de nacimiento",
        example = "2001-04-12"
    )
    LocalDate fechaNacimiento,

    @Schema(
        description = "Número telefónico",
        example = "987123456"
    )
    @Size(max = 20)
    String telefono,

    @Schema(
        description = "Correo electrónico",
        example = "lucia.mendoza@example.com"
    )
    @Email(message = "El correo electrónico no es válido")
    @Size(max = 120)
    String email,

    @Schema(
        description = "Dirección",
        example = "Av. Grau 250, Piura"
    )
    @Size(max = 255)
    String direccion

) {
}