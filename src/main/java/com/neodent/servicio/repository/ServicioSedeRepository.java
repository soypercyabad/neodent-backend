package com.neodent.servicio.repository;

import com.neodent.servicio.model.ServicioSede;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ServicioSedeRepository extends JpaRepository<ServicioSede, Long> {

    List<ServicioSede> findByIdServicioOrderByIdSedeAsc(Integer idServicio);

    List<ServicioSede> findByIdServicioInAndActivoTrue(Collection<Integer> ids);

    boolean existsByIdServicioAndIdSedeAndActivoTrue(Integer idServicio, Integer idSede);
}