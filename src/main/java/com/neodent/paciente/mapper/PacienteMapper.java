package com.neodent.paciente.mapper;

import com.neodent.paciente.dto.PacienteResponse;
import com.neodent.paciente.dto.PacienteResumenResponse;
import com.neodent.paciente.model.OrigenRegistro;
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

        OrigenRegistro origen = paciente.getOrigenRegistro();
        if (origen == null) {
            origen = paciente.getUsuario() != null ? OrigenRegistro.AUTORREGISTRO : OrigenRegistro.RECEPCION;
        }

        Integer sedeId = paciente.getSedeRegistro() != null ? paciente.getSedeRegistro().getId() : null;
        String sedeNombre = paciente.getSedeRegistro() != null ? paciente.getSedeRegistro().getNombre() : null;

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
            origen,
            sedeId,
            sedeNombre,
            paciente.getFechaCreacion(),
            paciente.getFechaActualizacion()
        );
    }

    public PacienteResumenResponse toResumenResponse(Paciente paciente) {
        String estadoCuenta = null;
        if (paciente.getUsuario() != null && paciente.getUsuario().getEstado() != null) {
            estadoCuenta = paciente.getUsuario().getEstado().getNombre();
        }

        OrigenRegistro origen = paciente.getOrigenRegistro();
        if (origen == null) {
            origen = paciente.getUsuario() != null ? OrigenRegistro.AUTORREGISTRO : OrigenRegistro.RECEPCION;
        }

        String sedeNombre = paciente.getSedeRegistro() != null ? paciente.getSedeRegistro().getNombre() : null;

        return new PacienteResumenResponse(
            paciente.getId(),
            paciente.getTipoDocumento().getCodigo(),
            paciente.getNumeroDocumento(),
            paciente.getNombres(),
            paciente.getApellidoPaterno(),
            paciente.getApellidoMaterno(),
            paciente.getTelefono(),
            paciente.getCorreo(),
            paciente.getActivo(),
            paciente.getUsuario() != null,
            estadoCuenta,
            origen,
            sedeNombre,
            paciente.getFechaCreacion()
        );
    }
}