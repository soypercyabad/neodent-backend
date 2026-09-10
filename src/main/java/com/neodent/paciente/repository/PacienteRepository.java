package com.neodent.paciente.repository;

import com.neodent.paciente.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    Optional<Paciente> findByTipoDocumentoCodigoAndNumeroDocumento(
        String codigo,
        String numeroDocumento
    );

    boolean existsByTipoDocumentoCodigoAndNumeroDocumento(
        String codigo,
        String numeroDocumento
    );
}