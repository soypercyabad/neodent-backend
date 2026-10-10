package com.neodent.usuario.repository;

import com.neodent.usuario.model.Rol;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombreAndActivoTrue(String nombre);
    
    Optional<Rol> findByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCase(String nombre);
    
    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Integer id);
    
    @EntityGraph(attributePaths = {"modulos"})
    List<Rol> findAllByOrderByNombreAsc();
    
    @EntityGraph(attributePaths = {"modulos"})
    List<Rol> findAllByActivoTrueOrderByNombreAsc();

    @Override
    @EntityGraph(attributePaths = {"modulos"})
    Optional<Rol> findById(Integer id);
}