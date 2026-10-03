package com.neodent.paciente.mapper;

import com.neodent.paciente.dto.PacienteResponse;
import com.neodent.paciente.model.Paciente;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {

    public PacienteResponse toResponse(Paciente paciente) {
        String estadoCuenta = null;
        Boolean correoVerificado = null;
        if (paciente.getUsuario() != null) {
            if (paciente.getUsuario().getEstado() != null) {
                estadoCuenta = paciente.getUsuario().getEstado().getNombre();
            }
            correoVerificado = paciente.getUsuario().getCorreoVerificado();
        }

        return new PacienteResponse(
            paciente.getId(),
            paciente.getTipoDocumento().getCodigo(),
            paciente.getNumeroDocumento(),
            paciente.getNombres(),
            paciente.getApellidoPaterno(),
            paciente.getApellidoMaterno(),
            paciente.getFechaNacimiento(),
            paciente.getTelefono(),
            paciente.getCorreo(),
            paciente.getDireccion(),
            paciente.getActivo(),
            paciente.getUsuario() != null ? paciente.getUsuario().getId() : null,
            paciente.getUsuario() != null,
            estadoCuenta,
            correoVerificado,
            paciente.getFechaCreacion(),
            paciente.getFechaActualizacion()
        );
    }
}