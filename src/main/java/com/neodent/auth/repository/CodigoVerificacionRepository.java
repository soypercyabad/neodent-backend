package com.neodent.auth.repository;

import com.neodent.auth.model.CodigoVerificacion;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoVerificacionRepository
        extends JpaRepository<CodigoVerificacion, Long> {

    @EntityGraph(attributePaths = {
        "usuario",
        "usuario.rol"
    })
    Optional<CodigoVerificacion> findByIdAndTipo(Long id, String tipo);

    Optional<CodigoVerificacion> findById(Long id);

    List<CodigoVerificacion> findAllByUsuarioIdAndTipoAndUsadoFalse(Long usuarioId, String tipo);

    @EntityGraph(attributePaths = "paciente")
    Optional<CodigoVerificacion> findByIdAndTipoAndPacienteIsNotNull(Long id, String tipo);
}