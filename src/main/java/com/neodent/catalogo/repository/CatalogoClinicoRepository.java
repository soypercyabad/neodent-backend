package com.neodent.catalogo.repository;

import com.neodent.catalogo.model.CatalogoClinico;
import com.neodent.catalogo.model.TipoCatalogoClinico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogoClinicoRepository extends JpaRepository<CatalogoClinico, Integer> {

    List<CatalogoClinico> findByActivoTrueOrderByNombreAsc();

    List<CatalogoClinico> findByTipoAndActivoTrueOrderByNombreAsc(TipoCatalogoClinico tipo);

    List<CatalogoClinico> findByTipoOrderByNombreAsc(TipoCatalogoClinico tipo);

    boolean existsByTipoAndNombreIgnoreCase(TipoCatalogoClinico tipo, String nombre);
}
