package com.neodent.usuario.repository;

import com.neodent.usuario.model.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombreAndActivoTrue(String nombre);
    
    Optional<Rol> findByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
    
    List<Rol> findAllByOrderByNombreAsc();
    
    List<Rol> findAllByActivoTrueOrderByNombreAsc();
}