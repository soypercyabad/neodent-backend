package com.neodent.paciente.repository;

import com.neodent.paciente.model.TipoDocumento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumento, Integer> {

    Optional<TipoDocumento> findByCodigoAndActivoTrue(String codigo);
}