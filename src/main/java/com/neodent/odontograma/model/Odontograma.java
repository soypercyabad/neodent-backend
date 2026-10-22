package com.neodent.odontograma.model;

import com.neodent.cita.model.Cita;
import com.neodent.historia.model.HistoriaClinica;
import com.neodent.odontologo.model.Odontologo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "odontograma")
@Getter
@Setter
@NoArgsConstructor
public class Odontograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_odontograma")
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

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Odontograma(HistoriaClinica historiaClinica, Cita cita, Odontologo odontologo, String observaciones) {
        this.historiaClinica = historiaClinica;
        this.cita = cita;
        this.odontologo = odontologo;
        this.observaciones = observaciones;
        this.fecha = LocalDateTime.now();
    }
}
