package com.neodent.audit;

import com.neodent.audit.model.Auditoria;
import com.neodent.audit.repository.AuditoriaRepository;
import com.neodent.usuario.model.Usuario;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(Long usuarioId, String accion, String entidad, Long idEntidad, String descripcion, String ip, String metadatosJson) {
        try {
            Usuario usuario = usuarioId != null ? usuarioRepository.findById(usuarioId).orElse(null) : null;
            Auditoria registro = new Auditoria(usuario, accion, entidad, idEntidad, descripcion, ip, metadatosJson);
            auditoriaRepository.save(registro);
            log.debug("Auditoría registrada: acción [{}] sobre entidad [{}] ID {}", accion, entidad, idEntidad);
        } catch (Exception ex) {
            log.warn("No se pudo guardar registro de auditoría para entidad {}: {}", entidad, ex.getMessage());
        }
    }
}
