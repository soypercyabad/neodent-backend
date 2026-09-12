package com.neodent.auth.repository;

import com.neodent.auth.model.TokenAccion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenAccionRepository
        extends JpaRepository<TokenAccion, Long> {

    List<TokenAccion>
    findAllByPacienteIdAndTipoAndUsadoFalseAndRevocadoFalse(
        Long pacienteId,
        String tipo
    );

    @EntityGraph(attributePaths = {
        "paciente",
        "paciente.tipoDocumento",
        "paciente.usuario"
    })
    Optional<TokenAccion>
    findByTokenHashAndTipoAndUsadoFalseAndRevocadoFalse(
        String tokenHash,
        String tipo
    );
}