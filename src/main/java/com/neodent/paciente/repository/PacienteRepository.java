package com.neodent.paciente.repository;

import com.neodent.paciente.model.Paciente;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByTelefono(String telefono);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByTelefonoAndIdNot(String telefono, Long id);

    @EntityGraph(attributePaths = {"tipoDocumento", "usuario"})
    @Query("""
        SELECT p
        FROM Paciente p
        WHERE (:activo IS NULL OR p.activo = :activo)
        AND (:conCuenta IS NULL OR
            (:conCuenta = true AND p.usuario IS NOT NULL) OR
            (:conCuenta = false AND p.usuario IS NULL))
        AND (
            :buscar IS NULL
            OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(p.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(p.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(p.correo) LIKE LOWER(CONCAT('%', :buscar, '%'))
            OR LOWER(p.telefono) LIKE LOWER(CONCAT('%', :buscar, '%'))
        )
    """)
    Page<Paciente> buscarPacientes(
        @Param("activo") Boolean activo,
        @Param("conCuenta") Boolean conCuenta,
        @Param("buscar") String buscar,
        Pageable pageable
    );
}