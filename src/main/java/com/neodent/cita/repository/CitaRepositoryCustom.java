package com.neodent.cita.repository;

import com.neodent.cita.model.Cita;

import java.time.LocalDateTime;
import java.util.List;

public interface CitaRepositoryCustom {

    List<Cita> buscarAgendaDinamica(
        Long odontologoId,
        String estado,
        Integer sedeId,
        LocalDateTime inicioDia,
        LocalDateTime finDia
    );
}
