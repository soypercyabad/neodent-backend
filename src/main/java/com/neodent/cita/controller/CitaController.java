package com.neodent.cita.controller;

import com.neodent.cita.dto.request.CancelarCitaRequest;
import com.neodent.cita.dto.request.ConfirmarCitaRequest;
import com.neodent.cita.dto.request.CrearReservaCitaRequest;
import com.neodent.cita.dto.request.MarcarNoAsistioRequest;
import com.neodent.cita.dto.request.ReprogramarCitaRequest;
import com.neodent.cita.dto.response.CitaResponse;
import com.neodent.cita.dto.response.DisponibilidadCitaResponse;
import com.neodent.cita.dto.response.ReservaCitaResponse;
import com.neodent.cita.service.CitaService;
import com.neodent.cita.service.DisponibilidadCitaService;
import com.neodent.cita.service.ReservaCitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.neodent.shared.response.PaginaResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "Gestión de agenda y citas odontológicas")
public class CitaController {

    private final ReservaCitaService reservaCitaService;
    private final CitaService citaService;
    private final DisponibilidadCitaService disponibilidadCitaService;


    @Operation(
        summary = "Reservar horario temporalmente",
        description = "Bloquea un horario durante 10 minutos mientras se completa el proceso de agendamiento."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Horario reservado temporalmente"),
        @ApiResponse(responseCode = "404", description = "Paciente, sede, servicio u odontólogo no encontrado"),
        @ApiResponse(responseCode = "409", description = "Horario no disponible")
    })
    @PostMapping("/hold")
    public ReservaCitaResponse crearHold(@Valid @RequestBody CrearReservaCitaRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        List<String> roles = jwt.getClaimAsStringList("roles");
        return reservaCitaService.crearHold(request, usuarioId, roles);
    }



    @Operation(
        summary = "Confirmar reserva y registrar cita",
        description = "Convierte una reserva temporal vigente en una cita con estado PROGRAMADA."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita registrada correctamente"),
        @ApiResponse(responseCode = "404", description = "Reserva o usuario no encontrado"),
        @ApiResponse(responseCode = "409", description = "Reserva expirada o horario no disponible")
    })
    @PostMapping("/confirm")
    public CitaResponse confirmarCita(@Valid @RequestBody ConfirmarCitaRequest request, @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        List<String> roles = jwt.getClaimAsStringList("roles");
        return citaService.confirmarCita(request.tokenReserva(), usuarioId, roles);
    }



    @Operation(
        summary = "Consultar horarios disponibles",
        description = """
            Obtiene los horarios disponibles de un odontólogo considerando horario de atención,
            duración del servicio, bloqueos, citas existentes y reservas temporales vigentes.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Disponibilidad obtenida correctamente"),
        @ApiResponse(responseCode = "404", description = "Odontólogo, sede o servicio no disponible"),
        @ApiResponse(responseCode = "409", description = "Datos incompatibles o fecha inválida")
    })
    @GetMapping("/disponibilidad")
    public DisponibilidadCitaResponse obtenerDisponibilidad(
        @RequestParam Long odontologoEspecialidadId,
        @RequestParam Integer sedeId,
        @RequestParam Integer servicioId,
        @RequestParam LocalDate fecha
    ) {
        return disponibilidadCitaService.obtenerDisponibilidad(odontologoEspecialidadId, sedeId, servicioId, fecha);
    }



    @Operation(
        summary = "Reprogramar cita",
        description = "Modifica la fecha y hora de una cita existente, registra el cambio en historial_cita y devuelve la cita a estado PROGRAMADA."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita reprogramada correctamente"),
        @ApiResponse(responseCode = "404", description = "Cita no encontrada"),
        @ApiResponse(responseCode = "409", description = "La cita o el nuevo horario no permiten reprogramación")
    })
    @PutMapping("/{citaId}/reprogramar")
    public CitaResponse reprogramarCita(
        @PathVariable Long citaId,
        @Valid @RequestBody ReprogramarCitaRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return citaService.reprogramarCita(citaId, request, usuarioId);
    }



    @Operation(
        summary = "Cancelar cita",
        description = "Cancela una cita en estado PROGRAMADA o CONFIRMADA y registra el cambio en historial_cita."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita cancelada correctamente"),
        @ApiResponse(responseCode = "404", description = "Cita no encontrada"),
        @ApiResponse(responseCode = "409", description = "La cita no puede cancelarse en su estado actual")
    })
    @PutMapping("/{citaId}/cancelar")
    public CitaResponse cancelarCita(
        @PathVariable Long citaId,
        @Valid @RequestBody CancelarCitaRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return citaService.cancelarCita(citaId, request, usuarioId);
    }



    @Operation(
        summary = "Marcar cita como no asistida",
        description = "Cambia una cita PROGRAMADA o CONFIRMADA al estado NO_ASISTIO y registra el cambio en historial_cita."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cita marcada como NO_ASISTIO correctamente"),
        @ApiResponse(responseCode = "404", description = "Cita no encontrada"),
        @ApiResponse(responseCode = "409", description = "La cita no permite este cambio de estado")
    })
    @PutMapping("/{citaId}/no-asistio")
    public CitaResponse marcarNoAsistio(
        @PathVariable Long citaId,
        @Valid @RequestBody MarcarNoAsistioRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return citaService.marcarNoAsistio(citaId, request, usuarioId);
    }



    @Operation(
        summary = "Confirmar asistencia",
        description = "Cambia una cita PROGRAMADA al estado CONFIRMADA, registra confirmada_en y guarda el cambio en historial_cita."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Asistencia confirmada correctamente"),
        @ApiResponse(responseCode = "404", description = "Cita no encontrada"),
        @ApiResponse(responseCode = "409", description = "La cita no puede confirmarse en su estado actual")
    })
    @PutMapping("/{citaId}/confirmar-asistencia")
    public CitaResponse confirmarAsistencia(@PathVariable Long citaId, @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return citaService.confirmarAsistencia(citaId, usuarioId);
    }



    @Operation(
        summary = "Iniciar atención",
        description = "Cambia una cita CONFIRMADA al estado EN_ATENCION y registra el cambio en historial_cita."
    )
    @PutMapping("/{citaId}/iniciar-atencion")
    public CitaResponse iniciarAtencion(@PathVariable Long citaId, @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return citaService.iniciarAtencion(citaId, usuarioId);
    }



    @Operation(
        summary = "Finalizar atención",
        description = "Cambia una cita EN_ATENCION al estado ATENDIDA y registra el cambio en historial_cita."
    )
    @PutMapping("/{citaId}/finalizar-atencion")
    public CitaResponse finalizarAtencion(@PathVariable Long citaId, @AuthenticationPrincipal Jwt jwt) {
        Long usuarioId = Long.valueOf(jwt.getSubject());
        return citaService.finalizarAtencion(citaId, usuarioId);
    }



    @Operation(
        summary = "Consultar mis citas",
        description = "Obtiene de forma paginada las citas asociadas al paciente autenticado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Citas obtenidas correctamente")
    })
    @GetMapping("/mis-citas")
    public PaginaResponse<CitaResponse> obtenerMisCitas(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "pagina", required = false) Integer pagina,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "tamano", required = false) Integer tamano,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        int p = pagina != null ? pagina : (page != null ? page : 0);
        int s = tamano != null ? tamano : (size != null ? size : 10);
        Long usuarioId = Long.valueOf(jwt.getSubject());
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "fechaHoraInicio"));
        return PaginaResponse.de(citaService.obtenerMisCitas(usuarioId, pageable));
    }



    @Operation(
        summary = "Consultar mi agenda",
        description = "Obtiene de forma paginada las citas asignadas al odontólogo autenticado."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agenda obtenida correctamente")
    })
    @GetMapping("/mi-agenda")
    public PaginaResponse<CitaResponse> obtenerMiAgenda(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(value = "pagina", required = false) Integer pagina,
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "tamano", required = false) Integer tamano,
        @RequestParam(value = "size", required = false) Integer size
    ) {
        int p = pagina != null ? pagina : (page != null ? page : 0);
        int s = tamano != null ? tamano : (size != null ? size : 10);
        Long usuarioId = Long.valueOf(jwt.getSubject());
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "fechaHoraInicio"));
        return PaginaResponse.de(citaService.obtenerMiAgenda(usuarioId, pageable));
    }


    @Operation(
        summary = "Consultar agenda operativa",
        description = "Obtiene las citas registradas y permite filtrar opcionalmente por fecha, odontólogo, estado y sede."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Agenda obtenida correctamente")
    })
    @GetMapping("/agenda")
    public List<CitaResponse> obtenerAgenda(
        @RequestParam(required = false) LocalDate fecha,
        @RequestParam(required = false) Long odontologoId,
        @RequestParam(required = false) String estado,
        @RequestParam(required = false) Integer sedeId
    ) {
        return citaService.obtenerAgenda(fecha, odontologoId, estado, sedeId);
    }
}