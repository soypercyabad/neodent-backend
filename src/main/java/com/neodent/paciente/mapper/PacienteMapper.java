package com.neodent.paciente.mapper;

import com.neodent.paciente.dto.PacienteResponse;
import com.neodent.paciente.model.Paciente;
import org.springframework.stereotype.Component;

@Component
public class PacienteMapper {

    public PacienteResponse toResponse(Paciente paciente) {

        return new PacienteResponse(
            paciente.getId(),
            paciente.getTipoDocumento().getCodigo(),
            paciente.getNumeroDocumento(),
            paciente.getNombres(),
            paciente.getApellidoPaterno(),
            paciente.getApellidoMaterno(),
            paciente.getTelefono(),
            paciente.getEmail(),
            paciente.getUsuario() != null
        );
    }
}