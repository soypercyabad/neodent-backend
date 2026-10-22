package com.neodent.historia.model;

import com.neodent.cita.model.Cita;
import com.neodent.odontologo.model.Odontologo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "evolucion_clinica")
@Getter
@Setter
@NoArgsConstructor
public class EvolucionClinica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evolucion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_historia_clinica", nullable = false)
    private HistoriaClinica historiaClinica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_odontologo", nullable = false)
    private Odontologo odontologo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    @Column(name = "diagnostico", columnDefinition = "TEXT")
    private String diagnostico;

    @Column(name = "procedimiento", columnDefinition = "TEXT")
    private String procedimiento;

    @Column(name = "indicaciones", columnDefinition = "TEXT")
    private String indicaciones;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    public EvolucionClinica(HistoriaClinica historiaClinica, Cita cita, Odontologo odontologo,
                            String diagnostico, String procedimiento, String indicaciones, String observaciones) {
        this.historiaClinica = historiaClinica;
        this.cita = cita;
        this.odontologo = odontologo;
        this.diagnostico = diagnostico;
        this.procedimiento = procedimiento;
        this.indicaciones = indicaciones;
        this.observaciones = observaciones;
        this.fecha = LocalDateTime.now();
    }
}
