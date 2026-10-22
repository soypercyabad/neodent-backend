package com.neodent.documento.model;

import com.neodent.cita.model.Cita;
import com.neodent.paciente.model.Paciente;
import com.neodent.usuario.model.Usuario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento")
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita")
    private Cita cita;

    @Column(name = "clave_almacenamiento", nullable = false, unique = true, length = 500)
    private String claveAlmacenamiento;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tipo_documento", nullable = false, length = 60)
    private String tipoDocumento;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "visible_paciente", nullable = false)
    private Boolean visiblePaciente = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    public Documento(Paciente paciente, Cita cita, String claveAlmacenamiento, String nombreOriginal,
                     String tipoMime, String tipoDocumento, Long tamanoBytes, Boolean visiblePaciente, Usuario usuario) {
        this.paciente = paciente;
        this.cita = cita;
        this.claveAlmacenamiento = claveAlmacenamiento;
        this.nombreOriginal = nombreOriginal;
        this.tipoMime = tipoMime;
        this.tipoDocumento = tipoDocumento;
        this.tamanoBytes = tamanoBytes;
        this.visiblePaciente = visiblePaciente != null ? visiblePaciente : false;
        this.usuario = usuario;
    }
}
