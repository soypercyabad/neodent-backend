package com.neodent.paciente.repository;

import com.neodent.paciente.model.Paciente;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PacienteRepository
        extends JpaRepository<Paciente, Long> {

    @Override
    @EntityGraph(attributePaths = "tipoDocumento")
    Optional<Paciente> findById(Long id);

    @EntityGraph(attributePaths = "tipoDocumento")
    Optional<Paciente> findByTipoDocumentoCodigoAndNumeroDocumento(String codigo, String numeroDocumento);

    @EntityGraph(attributePaths = {"tipoDocumento","usuario"})
    Optional<Paciente> findByUsuarioIdAndActivoTrue(Long usuarioId);

    boolean existsByTipoDocumentoCodigoAndNumeroDocumento(String codigo, String numeroDocumento);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByTelefono(String telefono);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);
}