package com.neodent.legal.repository;

import com.neodent.legal.model.AceptacionTerminos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AceptacionTerminosRepository extends JpaRepository<AceptacionTerminos, Long> {
    boolean existsByUsuarioIdAndTerminosId(Long usuarioId, Long terminosId);
    Optional<AceptacionTerminos> findFirstByUsuarioIdOrderByAceptadoEnDesc(Long usuarioId);
}
