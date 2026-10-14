package com.neodent.notification.repository;

import com.neodent.notification.model.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(Long usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true, n.leidaEn = :fecha WHERE n.usuario.id = :usuarioId AND n.leida = false")
    int marcarTodasComoLeidas(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDateTime fecha);
}
