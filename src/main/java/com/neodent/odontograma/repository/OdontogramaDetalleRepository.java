package com.neodent.odontograma.repository;

import com.neodent.odontograma.model.OdontogramaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OdontogramaDetalleRepository extends JpaRepository<OdontogramaDetalle, Long> {

    @Query("SELECT d FROM OdontogramaDetalle d " +
           "JOIN FETCH d.condicion c " +
           "WHERE d.odontograma.id = :odontogramaId " +
           "ORDER BY d.numeroPieza ASC, d.id ASC")
    List<OdontogramaDetalle> findByOdontogramaIdWithCondicion(@Param("odontogramaId") Long odontogramaId);

    List<OdontogramaDetalle> findByOdontogramaId(Long odontogramaId);

    Optional<OdontogramaDetalle> findByOdontogramaIdAndNumeroPiezaAndSuperficie(
        Long odontogramaId,
        Byte numeroPieza,
        String superficie
    );

    void deleteByOdontogramaIdAndNumeroPieza(Long odontogramaId, Byte numeroPieza);

    void deleteByOdontogramaIdAndNumeroPiezaAndSuperficie(Long odontogramaId, Byte numeroPieza, String superficie);
}
