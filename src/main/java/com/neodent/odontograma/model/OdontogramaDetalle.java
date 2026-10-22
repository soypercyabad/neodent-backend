package com.neodent.odontograma.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "odontograma_detalle")
@Getter
@Setter
@NoArgsConstructor
public class OdontogramaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_odontograma_detalle")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_odontograma", nullable = false)
    private Odontograma odontograma;

    @Column(name = "numero_pieza", nullable = false)
    private Byte numeroPieza;

    @Column(name = "superficie", length = 30)
    private String superficie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_condicion", nullable = false)
    private CondicionOdontograma condicion;

    @Column(name = "observacion", length = 255)
    private String observacion;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public OdontogramaDetalle(Odontograma odontograma, Byte numeroPieza, String superficie, CondicionOdontograma condicion, String observacion) {
        this.odontograma = odontograma;
        this.numeroPieza = numeroPieza;
        this.superficie = superficie;
        this.condicion = condicion;
        this.observacion = observacion;
    }

    public OdontogramaDetalle(Odontograma odontograma, Integer numeroPieza, String superficie, CondicionOdontograma condicion, String observacion) {
        this(odontograma, numeroPieza != null ? numeroPieza.byteValue() : null, superficie, condicion, observacion);
    }
}
