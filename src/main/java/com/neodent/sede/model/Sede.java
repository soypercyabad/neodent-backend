package com.neodent.sede.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sede")
@Getter @Setter
@NoArgsConstructor
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Integer id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(length = 80)
    private String distrito;

    @Column(length = 80)
    private String provincia;

    @Column(length = 80)
    private String departamento;

    @Column(length = 20)
    private String telefono;

    @Column(name = "correo", length = 120)
    private String email;

    @Column(name = "qr_pago_clave_almacenamiento", length = 255)
    private String qrPagoClaveAlmacenamiento;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(
        name = "fecha_creacion",
        insertable = false,
        updatable = false
    )
    private LocalDateTime fechaCreacion;
}