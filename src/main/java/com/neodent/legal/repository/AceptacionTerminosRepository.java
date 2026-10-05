package com.neodent.legal.repository;

import com.neodent.legal.model.AceptacionTerminos;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AceptacionTerminosRepository extends JpaRepository<AceptacionTerminos, Long> {
    boolean existsByUsuarioIdAndTerminosId(Long usuarioId, Long terminosId);
    Optional<AceptacionTerminos> findFirstByUsuarioIdOrderByAceptadoEnDesc(Long usuarioId);

    @Query("SELECT a FROM AceptacionTerminos a JOIN FETCH a.usuario JOIN FETCH a.terminos ORDER BY a.aceptadoEn DESC")
    List<AceptacionTerminos> findAllWithUsuarioAndTerminos();
}
