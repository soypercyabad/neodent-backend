package com.neodent.cita.repository;

import com.neodent.cita.model.Cita;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    Page<Cita> buscarAgendaPaginada(
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
    );
}
