package com.neodent.dashboard.service;

import com.neodent.cita.model.Cita;
import com.neodent.cita.repository.CitaRepository;
import com.neodent.dashboard.dto.response.*;
import com.neodent.paciente.repository.PacienteRepository;
import com.neodent.personal.repository.PersonalRepository;
import com.neodent.sede.repository.SedeRepository;
import com.neodent.servicio.repository.ServicioRepository;
import com.neodent.usuario.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final CitaRepository citaRepository;
    private final PacienteRepository pacienteRepository;
    private final PersonalRepository personalRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final ServicioRepository servicioRepository;

    private static final DateTimeFormatter DIA_MES_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    public AdminDashboardResponse obtenerAdmin(String periodo) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime desde = calcularFechaDesde(periodo, now);

        LocalDate hoy = now.toLocalDate();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        LocalDateTime inicioMes = hoy.withDayOfMonth(1).atStartOfDay();

        // 1. Citas del periodo ordenadas DESC
        List<Cita> citas = citaRepository.buscarCitasDashboardAdmin(desde);

        // 2. Conteo de KPIs consolidados
        long pacientesTotal = pacienteRepository.count();
        long pacientesActivos = pacienteRepository.countByActivoTrue();
        long pacientesNuevosMes = pacienteRepository.countByFechaCreacionGreaterThanEqual(inicioMes);

        long citasTotal = citas.size();
        long citasHoy = citaRepository.contarCitasEntre(inicioHoy, finHoy);

        long citasAtendidas = citas.stream().filter(c -> "ATENDIDA".equalsIgnoreCase(c.getEstado().getNombre())).count();
        long citasConfirmadas = citas.stream().filter(c -> "CONFIRMADA".equalsIgnoreCase(c.getEstado().getNombre())).count();
        long citasCanceladas = citas.stream().filter(c -> {
            String est = c.getEstado().getNombre();
            return "CANCELADA".equalsIgnoreCase(est) || "NO_ASISTIO".equalsIgnoreCase(est);
        }).count();
        long citasEnAtencion = citas.stream().filter(c -> "EN_ATENCION".equalsIgnoreCase(c.getEstado().getNombre())).count();

        BigDecimal facturacionEstimada = citas.stream()
            .filter(c -> {
                String est = c.getEstado().getNombre();
                return "ATENDIDA".equalsIgnoreCase(est) || "CONFIRMADA".equalsIgnoreCase(est);
            })
            .map(c -> c.getServicioSolicitado() != null ? c.getServicioSolicitado().getPrecioReferencial() : BigDecimal.ZERO)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long personalTotal = personalRepository.countByActivoTrue();
        long odontologosTotal = usuarioRepository.contarUsuariosActivosConRol("ODONTOLOGO");
        long recepcionistasTotal = usuarioRepository.contarUsuariosActivosConRol("RECEPCIONISTA");
        long sedesTotal = sedeRepository.count();
        long serviciosTotal = servicioRepository.count();

        AdminDashboardResponse.DashboardKpis kpis = new AdminDashboardResponse.DashboardKpis(
            pacientesTotal,
            pacientesActivos,
            pacientesNuevosMes,
            citasTotal,
            citasAtendidas,
            citasConfirmadas,
            citasCanceladas,
            citasEnAtencion,
            citasHoy,
            facturacionEstimada,
            personalTotal,
            odontologosTotal,
            recepcionistasTotal,
            sedesTotal,
            serviciosTotal
        );

        // 3. Gráfico Evolución temporal (cronológico)
        List<Cita> citasCronologicas = new ArrayList<>(citas);
        citasCronologicas.sort(Comparator.comparing(Cita::getFechaHoraInicio));

        Map<String, int[]> agrupadoFecha = new LinkedHashMap<>();
        for (Cita c : citasCronologicas) {
            String fechaKey = c.getFechaHoraInicio().format(DIA_MES_FORMATTER);
            int[] vals = agrupadoFecha.computeIfAbsent(fechaKey, k -> new int[2]); // [0]=programadas/total, [1]=atendidas
            vals[0]++;
            if ("ATENDIDA".equalsIgnoreCase(c.getEstado().getNombre())) {
                vals[1]++;
            }
        }

        List<AdminDashboardResponse.EvolucionItem> evolucion = agrupadoFecha.entrySet().stream()
            .map(e -> new AdminDashboardResponse.EvolucionItem(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList();

        if (evolucion.isEmpty()) {
            evolucion = List.of(
                new AdminDashboardResponse.EvolucionItem("Inicio", 0, 0),
                new AdminDashboardResponse.EvolucionItem("Actual", 0, 0)
            );
        }

        // 4. Top 5 Servicios más solicitados
        Map<String, Integer> serviciosConteo = new HashMap<>();
        for (Cita c : citas) {
            String nom = (c.getServicioSolicitado() != null) ? c.getServicioSolicitado().getNombre() : "Consulta General";
            serviciosConteo.merge(nom, 1, Integer::sum);
        }

        List<AdminDashboardResponse.TopServicioItem> topServicios = serviciosConteo.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> new AdminDashboardResponse.TopServicioItem(e.getKey(), e.getValue()))
            .toList();

        // 5. Distribución de Estados
        Map<String, Integer> estadosConteo = new LinkedHashMap<>();
        estadosConteo.put("Confirmadas", 0);
        estadosConteo.put("Atendidas", 0);
        estadosConteo.put("En atención", 0);
        estadosConteo.put("Canceladas", 0);
        estadosConteo.put("Pendientes", 0);

        for (Cita c : citas) {
            String est = c.getEstado().getNombre();
            if ("CONFIRMADA".equalsIgnoreCase(est)) estadosConteo.merge("Confirmadas", 1, Integer::sum);
            else if ("ATENDIDA".equalsIgnoreCase(est)) estadosConteo.merge("Atendidas", 1, Integer::sum);
            else if ("EN_ATENCION".equalsIgnoreCase(est)) estadosConteo.merge("En atención", 1, Integer::sum);
            else if ("CANCELADA".equalsIgnoreCase(est) || "NO_ASISTIO".equalsIgnoreCase(est)) estadosConteo.merge("Canceladas", 1, Integer::sum);
            else estadosConteo.merge("Pendientes", 1, Integer::sum);
        }

        Map<String, String> coloresEstados = Map.of(
            "Atendidas", "#10B981",
            "Confirmadas", "#3B82F6",
            "En atención", "#8B5CF6",
            "Canceladas", "#EF4444",
            "Pendientes", "#F59E0B"
        );

        List<AdminDashboardResponse.EstadoDistribucionItem> distribucionEstados = estadosConteo.entrySet().stream()
            .filter(e -> e.getValue() > 0)
            .map(e -> new AdminDashboardResponse.EstadoDistribucionItem(
                e.getKey(),
                e.getValue(),
                coloresEstados.getOrDefault(e.getKey(), "#64748B")
            ))
            .toList();

        // 6. Productividad Top 5 Odontólogos
        Map<String, int[]> doctoresConteo = new HashMap<>();
        for (Cita c : citas) {
            String doc = (c.getOdontologoEspecialidad() != null && c.getOdontologoEspecialidad().getOdontologo() != null)
                ? c.getOdontologoEspecialidad().getOdontologo().getPersonal().getNombres() + " " +
                  c.getOdontologoEspecialidad().getOdontologo().getPersonal().getApellidoPaterno()
                : "Por asignar";
            int[] vals = doctoresConteo.computeIfAbsent(doc, k -> new int[2]); // [0]=total, [1]=atendidas
            vals[0]++;
            if ("ATENDIDA".equalsIgnoreCase(c.getEstado().getNombre())) {
                vals[1]++;
            }
        }

        List<AdminDashboardResponse.DoctorProductividadItem> productividadDoctores = doctoresConteo.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue()[0], a.getValue()[0]))
            .limit(5)
            .map(e -> new AdminDashboardResponse.DoctorProductividadItem(e.getKey(), e.getValue()[0], e.getValue()[1]))
            .toList();

        // 7. Demanda por Sedes
        Map<String, Integer> sedesConteo = new HashMap<>();
        for (Cita c : citas) {
            String sedeNom = (c.getSede() != null) ? c.getSede().getNombre() : "Sede Principal";
            sedesConteo.merge(sedeNom, 1, Integer::sum);
        }

        List<AdminDashboardResponse.SedeDemandaItem> sedesData = sedesConteo.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(e -> new AdminDashboardResponse.SedeDemandaItem(e.getKey(), e.getValue()))
            .toList();

        // 8. Citas Recientes (máximo 8 citas para mostrar en vivo en el dashboard)
        List<DashboardCitaResumen> citasRecientes = citas.stream()
            .limit(8)
            .map(this::mapearResumen)
            .toList();

        return new AdminDashboardResponse(
            kpis,
            evolucion,
            topServicios,
            distribucionEstados,
            productividadDoctores,
            sedesData,
            citasRecientes
        );
    }

    public DentistDashboardResponse obtenerOdontologo(Long usuarioId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate hoy = now.toLocalDate();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);

        List<Cita> citas = citaRepository.buscarCitasDashboardOdontologo(usuarioId, inicioHoy.minusDays(30));

        int citasHoy = (int) citas.stream()
            .filter(c -> !c.getFechaHoraInicio().isBefore(inicioHoy) && c.getFechaHoraInicio().isBefore(finHoy))
            .count();

        int citasAtendidasHoy = (int) citas.stream()
            .filter(c -> !c.getFechaHoraInicio().isBefore(inicioHoy) && c.getFechaHoraInicio().isBefore(finHoy))
            .filter(c -> "ATENDIDA".equalsIgnoreCase(c.getEstado().getNombre()))
            .count();

        int citasProximas = (int) citas.stream()
            .filter(c -> !c.getFechaHoraInicio().isBefore(now))
            .filter(c -> !"CANCELADA".equalsIgnoreCase(c.getEstado().getNombre()))
            .count();

        int pacientesUnicos = (int) citas.stream()
            .map(c -> c.getPaciente().getId())
            .distinct()
            .count();

        List<DashboardCitaResumen> resumenCitas = citas.stream()
            .limit(10)
            .map(this::mapearResumen)
            .toList();

        return new DentistDashboardResponse(
            citasHoy,
            citasAtendidasHoy,
            citasProximas,
            pacientesUnicos,
            resumenCitas
        );
    }

    public ReceptionDashboardResponse obtenerRecepcion() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate hoy = now.toLocalDate();
        LocalDateTime inicioHoy = hoy.atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        LocalDateTime haceUnaSemana = now.minusDays(7);

        List<Cita> citas = citaRepository.buscarCitasDashboardRecepcion(inicioHoy.minusDays(1));

        List<Cita> citasDeHoy = citas.stream()
            .filter(c -> !c.getFechaHoraInicio().isBefore(inicioHoy) && c.getFechaHoraInicio().isBefore(finHoy))
            .toList();

        int citasHoy = citasDeHoy.size();
        int citasPendientesHoy = (int) citasDeHoy.stream()
            .filter(c -> {
                String est = c.getEstado().getNombre();
                return "PROGRAMADA".equalsIgnoreCase(est) || "CONFIRMADA".equalsIgnoreCase(est);
            }).count();

        int citasAtendidasHoy = (int) citasDeHoy.stream()
            .filter(c -> "ATENDIDA".equalsIgnoreCase(c.getEstado().getNombre()))
            .count();

        int citasEnAtencionHoy = (int) citasDeHoy.stream()
            .filter(c -> "EN_ATENCION".equalsIgnoreCase(c.getEstado().getNombre()))
            .count();

        int citasPendientesTotal = (int) citas.stream()
            .filter(c -> {
                String est = c.getEstado().getNombre();
                return "PROGRAMADA".equalsIgnoreCase(est) || "CONFIRMADA".equalsIgnoreCase(est);
            }).count();

        List<Cita> citasProximas = citas.stream()
            .filter(c -> !c.getFechaHoraInicio().isBefore(now.minusHours(12)))
            .filter(c -> {
                String est = c.getEstado().getNombre();
                return !"CANCELADA".equalsIgnoreCase(est) && !"NO_ASISTIO".equalsIgnoreCase(est);
            })
            .toList();

        int citasProximasTotal = citasProximas.size();
        long pacientesNuevosSemana = pacienteRepository.countByFechaCreacionGreaterThanEqual(haceUnaSemana);

        List<DashboardCitaResumen> resHoy = citasDeHoy.stream().map(this::mapearResumen).toList();
        List<DashboardCitaResumen> resProximas = citasProximas.stream().limit(8).map(this::mapearResumen).toList();

        return new ReceptionDashboardResponse(
            citasHoy,
            citasPendientesHoy,
            citasAtendidasHoy,
            citasEnAtencionHoy,
            citasPendientesTotal,
            citasProximasTotal,
            pacientesNuevosSemana,
            resHoy,
            resProximas
        );
    }

    private DashboardCitaResumen mapearResumen(Cita c) {
        String pacienteNom = c.getPaciente().getNombres() + " " + c.getPaciente().getApellidoPaterno();
        String pacienteTipoDoc = (c.getPaciente().getTipoDocumento() != null) ? c.getPaciente().getTipoDocumento().getCodigo() : "DNI";
        String pacienteNumDoc = c.getPaciente().getNumeroDocumento();
        String pacienteTel = c.getPaciente().getTelefono();
        String servicioNom = (c.getServicioSolicitado() != null) ? c.getServicioSolicitado().getNombre() : "Consulta General";
        String odontologoNom = (c.getOdontologoEspecialidad() != null && c.getOdontologoEspecialidad().getOdontologo() != null)
            ? c.getOdontologoEspecialidad().getOdontologo().getPersonal().getNombres() + " " +
              c.getOdontologoEspecialidad().getOdontologo().getPersonal().getApellidoPaterno()
            : "Por asignar";
        String sedeNom = (c.getSede() != null) ? c.getSede().getNombre() : "Sede Principal";
        BigDecimal precio = (c.getServicioSolicitado() != null) ? c.getServicioSolicitado().getPrecioReferencial() : BigDecimal.ZERO;

        return new DashboardCitaResumen(
            c.getId(),
            c.getFechaHoraInicio(),
            c.getFechaHoraFin(),
            c.getPaciente().getId(),
            pacienteNom,
            pacienteTipoDoc,
            pacienteNumDoc,
            pacienteTel,
            servicioNom,
            odontologoNom,
            sedeNom,
            c.getEstado().getNombre(),
            precio
        );
    }

    private LocalDateTime calcularFechaDesde(String periodo, LocalDateTime now) {
        if (periodo == null) return now.minusDays(30);
        return switch (periodo.toUpperCase().trim()) {
            case "7D" -> now.minusDays(7);
            case "30D" -> now.minusDays(30);
            case "AÑO", "ANO", "1Y" -> now.minusYears(1);
            case "TODO" -> null;
            default -> now.minusDays(30);
        };
    }
}
