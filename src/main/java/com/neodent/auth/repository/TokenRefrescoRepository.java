package com.neodent.auth.repository;

import com.neodent.auth.model.TokenRefresco;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"usuario", "usuario.roles", "usuario.estado"})
    Optional<TokenRefresco> findByHashToken(String hashToken);

    List<TokenRefresco> findAllByUsuarioIdAndRevocadoFalse(Long usuarioId);
}