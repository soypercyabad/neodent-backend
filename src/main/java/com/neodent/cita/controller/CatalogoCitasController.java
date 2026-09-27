package com.neodent.cita.controller;

import com.neodent.especialidad.repository.OdontologoEspecialidadRepository;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.servicio.model.ServicioSede;
import com.neodent.servicio.repository.ServicioRepository;
import com.neodent.servicio.repository.ServicioSedeRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/citas/catalogo")
@RequiredArgsConstructor
@Tag(name = "Catálogo de citas", description = "Operaciones de catálogo de citas")
public class CatalogoCitasController {

    private final ServicioRepository servicioRepository;
    private final SedeRepository sedeRepository;
    private final OdontologoEspecialidadRepository odontologoEspecialidadRepository;
    private final ServicioSedeRepository servicioSedeRepository;

    public record ServicioItem(
        Integer id,
        String nombre,
        String descripcion,
        Integer especialidadId,
        Integer duracionMinutos,
        BigDecimal precioReferencial,
        List<Integer> sedeIds
    ) {}

    public record SedeItem(
        Integer id,
        String nombre,
        String direccion
    ) {}

    public record EspecialistaItem(
        Long odontologoEspecialidadId,
        Long odontologoId,
        String nombres,
        String apellidoPaterno,
        String apellidoMaterno,
        Integer especialidadId,
        String especialidad,
        boolean tieneFoto
    ) {}



    @Operation(
        summary = "Listar catálogo de servicios",
        description = "Retorna la lista de servicios activos asociados a sedes activas"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Servicios de catálogo listados exitosamente")
    })
    @GetMapping("/servicios")
    @Transactional(readOnly = true)
    public List<ServicioItem> servicios() {
        var servicios = servicioRepository.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getActivo()))
            .filter(s -> Boolean.TRUE.equals(s.getEspecialidad().getActivo()))
            .toList();

        Set<Integer> sedesActivas = sedeRepository.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getActivo()))
            .map(s -> s.getId())
            .collect(Collectors.toSet());

        Map<Integer, List<Integer>> asociaciones = servicioSedeRepository
            .findByIdServicioInAndActivoTrue(
                servicios.stream().map(s -> s.getId()).toList()
            )
            .stream()
            .filter(r -> sedesActivas.contains(r.getIdSede()))
            .collect(Collectors.groupingBy(
                ServicioSede::getIdServicio,
                Collectors.mapping(ServicioSede::getIdSede, Collectors.toList())
            ));

        return servicios.stream()
            .filter(s -> !asociaciones.getOrDefault(s.getId(), List.of()).isEmpty())
            .sorted(Comparator.comparing(s -> s.getNombre().toLowerCase()))
            .map(s -> new ServicioItem(
                s.getId(),
                s.getNombre(),
                s.getDescripcion(),
                s.getEspecialidad().getId(),
                s.getDuracionMinutos().intValue(),
                s.getPrecioReferencial(),
                asociaciones.getOrDefault(s.getId(), List.of())
            ))
            .toList();
    }



    @Operation(
        summary = "Listar catálogo de sedes",
        description = "Retorna la lista de sedes activas para el agendamiento de citas"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sedes de catálogo listadas exitosamente")
    })
    @GetMapping("/sedes")
    @Transactional(readOnly = true)
    public List<SedeItem> sedes() {
        return sedeRepository.findAll().stream()
            .filter(s -> Boolean.TRUE.equals(s.getActivo()))
            .sorted(Comparator.comparing(s -> s.getNombre().toLowerCase()))
            .map(s -> new SedeItem(
                s.getId(),
                s.getNombre(),
                s.getDireccion()
            ))
            .toList();
    }



    @Operation(
        summary = "Listar especialistas por especialidad",
        description = "Retorna la lista de odontólogos activos asociados a una especialidad"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Especialistas listados exitosamente")
    })
    @GetMapping("/especialistas")
    @Transactional(readOnly = true)
    public List<EspecialistaItem> especialistas(
        @Parameter(description = "ID de la especialidad", example = "1")
        @RequestParam Integer especialidadId
    ) {
        return odontologoEspecialidadRepository.findAll().stream()
            .filter(oe -> Boolean.TRUE.equals(oe.getActivo()))
            .filter(oe -> Boolean.TRUE.equals(oe.getOdontologo().getActivo()))
            .filter(oe -> Boolean.TRUE.equals(oe.getOdontologo().getPersonal().getActivo()))
            .filter(oe -> oe.getEspecialidad().getId().equals(especialidadId))
            .map(oe -> new EspecialistaItem(
                oe.getId(),
                oe.getOdontologo().getId(),
                oe.getOdontologo().getPersonal().getNombres(),
                oe.getOdontologo().getPersonal().getApellidoPaterno(),
                oe.getOdontologo().getPersonal().getApellidoMaterno(),
                oe.getEspecialidad().getId(),
                oe.getEspecialidad().getNombre(),
                oe.getOdontologo().getFotoNombreArchivo() != null && !oe.getOdontologo().getFotoNombreArchivo().isBlank()
            ))
            .toList();
    }
}