package com.neodent.cita.repository;

import com.neodent.cita.model.Cita;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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

    @Override
    public Page<Cita> buscarAgendaPaginada(
        Long odontologoId,
        Long odontologoUsuarioId,
        String estado,
        Integer sedeId,
        LocalDateTime inicio,
        LocalDateTime fin,
        List<String> estadosExcluidos,
        List<String> estadosIncluidos,
        String buscar,
        Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // 1. COUNT QUERY
        CriteriaQuery<Long> countCq = cb.createQuery(Long.class);
        Root<Cita> countRoot = countCq.from(Cita.class);
        countCq.select(cb.count(countRoot));

        List<Predicate> countPredicates = construirPredicados(
            cb, countRoot, odontologoId, odontologoUsuarioId, estado,
            sedeId, inicio, fin, estadosExcluidos, estadosIncluidos, buscar
        );
        if (!countPredicates.isEmpty()) {
            countCq.where(countPredicates.toArray(new Predicate[0]));
        }
        Long total = entityManager.createQuery(countCq).getSingleResult();

        if (total == 0L) {
            return new PageImpl<>(List.of(), pageable, 0L);
        }

        // 2. DATA QUERY
        CriteriaQuery<Cita> cq = cb.createQuery(Cita.class);
        Root<Cita> root = cq.from(Cita.class);

        root.fetch("estado", JoinType.INNER);
        root.fetch("paciente", JoinType.INNER);
        Fetch<Object, Object> oe = root.fetch("odontologoEspecialidad", JoinType.INNER);
        Fetch<Object, Object> o = oe.fetch("odontologo", JoinType.INNER);
        o.fetch("personal", JoinType.INNER);
        oe.fetch("especialidad", JoinType.INNER);
        root.fetch("sede", JoinType.INNER);
        root.fetch("servicioSolicitado", JoinType.LEFT);

        List<Predicate> dataPredicates = construirPredicados(
            cb, root, odontologoId, odontologoUsuarioId, estado,
            sedeId, inicio, fin, estadosExcluidos, estadosIncluidos, buscar
        );
        if (!dataPredicates.isEmpty()) {
            cq.where(dataPredicates.toArray(new Predicate[0]));
        }

        // Ordenamiento dinámico
        List<Order> orders = new ArrayList<>();
        if (pageable.getSort().isSorted()) {
            for (Sort.Order sortOrder : pageable.getSort()) {
                Path<?> sortPath = switch (sortOrder.getProperty()) {
                    case "idCita", "id" -> root.get("id");
                    case "estado" -> root.get("estado").get("nombre");
                    case "fechaHoraFin" -> root.get("fechaHoraFin");
                    default -> root.get("fechaHoraInicio");
                };
                orders.add(sortOrder.isAscending() ? cb.asc(sortPath) : cb.desc(sortPath));
            }
        } else {
            orders.add(cb.desc(root.get("fechaHoraInicio")));
        }
        cq.orderBy(orders);

        TypedQuery<Cita> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Cita> resultList = query.getResultList();
        return new PageImpl<>(resultList, pageable, total);
    }

    private List<Predicate> construirPredicados(
        CriteriaBuilder cb,
        Root<Cita> root,
        Long odontologoId,
        Long odontologoUsuarioId,
        String estado,
        Integer sedeId,
        LocalDateTime inicio,
        LocalDateTime fin,
        List<String> estadosExcluidos,
        List<String> estadosIncluidos,
        String buscar
    ) {
        List<Predicate> predicates = new ArrayList<>();

        if (odontologoId != null) {
            predicates.add(cb.equal(
                root.get("odontologoEspecialidad").get("odontologo").get("id"),
                odontologoId
            ));
        }

        if (odontologoUsuarioId != null) {
            predicates.add(cb.equal(
                root.get("odontologoEspecialidad").get("odontologo").get("personal").get("usuario").get("id"),
                odontologoUsuarioId
            ));
        }

        if (estado != null && !estado.isBlank()) {
            predicates.add(cb.equal(
                cb.upper(root.get("estado").get("nombre")),
                estado.toUpperCase().trim()
            ));
        }

        if (sedeId != null) {
            predicates.add(cb.equal(
                root.get("sede").get("id"),
                sedeId
            ));
        }

        if (inicio != null) {
            predicates.add(cb.greaterThanOrEqualTo(
                root.get("fechaHoraInicio"),
                inicio
            ));
        }

        if (fin != null) {
            predicates.add(cb.lessThan(
                root.get("fechaHoraInicio"),
                fin
            ));
        }

        if (estadosExcluidos != null && !estadosExcluidos.isEmpty()) {
            predicates.add(root.get("estado").get("nombre").in(estadosExcluidos).not());
        }

        if (estadosIncluidos != null && !estadosIncluidos.isEmpty()) {
            predicates.add(root.get("estado").get("nombre").in(estadosIncluidos));
        }

        if (buscar != null && !buscar.isBlank()) {
            String rawSearch = buscar.trim().toLowerCase();
            String cleanDoctorSearch = rawSearch.replaceAll("(?i)^dr\\.?\\s*", "").trim();
            String pattern = "%" + rawSearch + "%";
            String doctorPattern = "%" + (cleanDoctorSearch.isEmpty() ? rawSearch : cleanDoctorSearch) + "%";

            // Paciente: Nombres + Apellidos (y orden inverso apellidos primero)
            Expression<String> pacNomCompleto = cb.concat(
                cb.concat(cb.lower(root.get("paciente").get("nombres")), " "),
                cb.concat(
                    cb.lower(root.get("paciente").get("apellidoPaterno")),
                    cb.concat(" ", cb.coalesce(cb.lower(root.get("paciente").get("apellidoMaterno")), ""))
                )
            );
            Expression<String> pacApellidosPrimero = cb.concat(
                cb.concat(
                    cb.lower(root.get("paciente").get("apellidoPaterno")),
                    cb.concat(" ", cb.coalesce(cb.lower(root.get("paciente").get("apellidoMaterno")), ""))
                ),
                cb.concat(" ", cb.lower(root.get("paciente").get("nombres")))
            );

            // Odontólogo: Nombres + Apellidos de Personal
            Path<?> personal = root.get("odontologoEspecialidad").get("odontologo").get("personal");
            Expression<String> docNomCompleto = cb.concat(
                cb.concat(cb.lower(personal.get("nombres")), " "),
                cb.concat(
                    cb.lower(personal.get("apellidoPaterno")),
                    cb.concat(" ", cb.coalesce(cb.lower(personal.get("apellidoMaterno")), ""))
                )
            );
            Expression<String> docApellidosPrimero = cb.concat(
                cb.concat(
                    cb.lower(personal.get("apellidoPaterno")),
                    cb.concat(" ", cb.coalesce(cb.lower(personal.get("apellidoMaterno")), ""))
                ),
                cb.concat(" ", cb.lower(personal.get("nombres")))
            );

            List<Predicate> searchPreds = new ArrayList<>();

            // 1. Coincidencia directa sobre nombre completo o apellidos primero
            searchPreds.add(cb.like(pacNomCompleto, pattern));
            searchPreds.add(cb.like(pacApellidosPrimero, pattern));
            searchPreds.add(cb.like(docNomCompleto, doctorPattern));
            searchPreds.add(cb.like(docApellidosPrimero, doctorPattern));

            // 2. Coincidencia por campos individuales
            searchPreds.add(cb.like(cb.lower(root.get("paciente").get("nombres")), pattern));
            searchPreds.add(cb.like(cb.lower(root.get("paciente").get("apellidoPaterno")), pattern));
            searchPreds.add(cb.like(cb.lower(root.get("paciente").get("apellidoMaterno")), pattern));
            searchPreds.add(cb.like(root.get("paciente").get("numeroDocumento"), pattern));

            searchPreds.add(cb.like(cb.lower(personal.get("nombres")), doctorPattern));
            searchPreds.add(cb.like(cb.lower(personal.get("apellidoPaterno")), doctorPattern));
            searchPreds.add(cb.like(cb.lower(root.get("odontologoEspecialidad").get("especialidad").get("nombre")), pattern));
            searchPreds.add(cb.like(cb.lower(root.get("servicioSolicitado").get("nombre")), pattern));

            // 3. Soporte para búsqueda por palabras (tokens): ej. "Percy Molina" o "Juan Gomez"
            String[] tokens = rawSearch.split("\\s+");
            if (tokens.length > 1) {
                List<Predicate> pacTokenPreds = new ArrayList<>();
                for (String token : tokens) {
                    if (!token.isBlank()) {
                        pacTokenPreds.add(cb.like(pacNomCompleto, "%" + token + "%"));
                    }
                }
                if (!pacTokenPreds.isEmpty()) {
                    searchPreds.add(cb.and(pacTokenPreds.toArray(new Predicate[0])));
                }

                List<Predicate> docTokenPreds = new ArrayList<>();
                for (String token : tokens) {
                    if (!token.isBlank() && !token.equalsIgnoreCase("dr") && !token.equalsIgnoreCase("dr.")) {
                        docTokenPreds.add(cb.like(docNomCompleto, "%" + token + "%"));
                    }
                }
                if (!docTokenPreds.isEmpty()) {
                    searchPreds.add(cb.and(docTokenPreds.toArray(new Predicate[0])));
                }
            }

            // 4. Búsqueda por ID numérico o código tipo CITA-00010
            String cleanNumeric = rawSearch.replaceAll("(?i)[#cita\\-\\s]", "");
            if (!cleanNumeric.isBlank() && cleanNumeric.matches("\\d+")) {
                try {
                    searchPreds.add(cb.equal(root.get("id"), Long.valueOf(cleanNumeric)));
                } catch (NumberFormatException ignored) {}
            }

            predicates.add(cb.or(searchPreds.toArray(new Predicate[0])));
        }

        return predicates;
    }
}
