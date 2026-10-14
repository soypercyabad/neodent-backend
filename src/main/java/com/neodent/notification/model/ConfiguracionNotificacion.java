package com.neodent.notification.model;

import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_notificacion")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion")
    private Integer id;

    @Column(nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(name = "canal_correo", nullable = false)
    private boolean canalCorreo = true;

    @Column(name = "canal_portal", nullable = false)
    private boolean canalPortal = true;

    @Column(name = "anticipacion_valor", nullable = false)
    private int anticipacionValor = 24;

    @Column(name = "anticipacion_unidad", nullable = false, length = 20)
    private String anticipacionUnidad = "HORAS";

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_actualizacion")
    private Usuario actualizadoPor;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
