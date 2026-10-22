package com.neodent.documento.repository;

import com.neodent.documento.model.Documento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentoRepository extends JpaRepository<Documento, Long> {

    List<Documento> findByPacienteIdOrderByFechaCreacionDesc(Long pacienteId);

    List<Documento> findByPacienteIdAndTipoDocumentoOrderByFechaCreacionDesc(Long pacienteId, String tipoDocumento);

    Optional<Documento> findByClaveAlmacenamiento(String clave);
}
