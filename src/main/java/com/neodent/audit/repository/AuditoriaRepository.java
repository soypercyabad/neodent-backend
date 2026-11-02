package com.neodent.audit.repository;

import com.neodent.audit.model.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    List<Auditoria> findByEntidadAndIdEntidadOrderByFechaCreacionDesc(String entidad, Long idEntidad);
}
