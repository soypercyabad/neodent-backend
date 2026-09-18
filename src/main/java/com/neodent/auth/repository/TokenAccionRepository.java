package com.neodent.auth.repository;

import com.neodent.auth.model.TokenAccion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenAccionRepository extends JpaRepository<TokenAccion, Long> {

    List<TokenAccion> findAllByPacienteIdAndTipoAndUsadoFalseAndRevocadoFalse(Long pacienteId, String tipo);

    List<TokenAccion> findAllByUsuarioIdAndTipoAndUsadoFalseAndRevocadoFalse(Long usuarioId, String tipo);

    @EntityGraph(attributePaths = {"paciente", "paciente.tipoDocumento", "paciente.usuario"})
    Optional<TokenAccion> findByHashTokenAndTipoAndUsadoFalseAndRevocadoFalse(String hashToken, String tipo);
}