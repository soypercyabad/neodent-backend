package com.neodent.usuario.repository;

import com.neodent.usuario.model.Modulo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModuloRepository extends JpaRepository<Modulo, Integer> {

    List<Modulo> findAllByActivoTrueOrderByOrdenAsc();

    List<Modulo> findAllByIdInAndActivoTrue(Collection<Integer> ids);

    Optional<Modulo> findByCodigoIgnoreCase(String codigo);
}
