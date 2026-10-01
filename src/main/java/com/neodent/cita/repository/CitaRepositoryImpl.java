package com.neodent.cita.repository;

import com.neodent.cita.model.Cita;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CitaRepositoryImpl implements CitaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Cita> buscarAgendaDinamica(
        Long odontologoId,
        String estado,
        Integer sedeId,
        LocalDateTime inicioDia,
        LocalDateTime finDia
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Cita> cq = cb.createQuery(Cita.class);
        Root<Cita> c = cq.from(Cita.class);

        c.fetch("estado", JoinType.INNER);
        c.fetch("paciente", JoinType.INNER);
        Fetch<Object, Object> oe = c.fetch("odontologoEspecialidad", JoinType.INNER);
        oe.fetch("odontologo", JoinType.INNER);
        oe.fetch("especialidad", JoinType.INNER);
        c.fetch("sede", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        if (odontologoId != null) {
            predicates.add(cb.equal(
                c.get("odontologoEspecialidad").get("odontologo").get("id"),
                odontologoId
            ));
        }

        if (estado != null && !estado.isBlank()) {
            predicates.add(cb.equal(
                c.get("estado").get("nombre"),
                estado
            ));
        }

        if (sedeId != null) {
            predicates.add(cb.equal(
                c.get("sede").get("id"),
                sedeId
            ));
        }

        if (inicioDia != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                c.get("fechaHoraInicio"),
                inicioDia
            ));
        }

        if (finDia != null) {
            predicates.add(cb.lessThan(
                c.get("fechaHoraInicio"),
                finDia
            ));
        }

        if (!predicates.isEmpty()) {
            cq.where(predicates.toArray(new Predicate[0]));
        }

        cq.orderBy(cb.desc(c.get("fechaHoraInicio")));

        return entityManager.createQuery(cq).getResultList();
    }
}
