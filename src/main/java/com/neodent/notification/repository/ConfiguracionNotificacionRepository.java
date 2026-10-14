package com.neodent.notification.repository;

import com.neodent.notification.model.ConfiguracionNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionNotificacionRepository extends JpaRepository<ConfiguracionNotificacion, Integer> {

    Optional<ConfiguracionNotificacion> findByCodigo(String codigo);
}
