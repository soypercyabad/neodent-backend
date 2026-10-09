package com.neodent.usuario.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "rol")
@Getter
@Setter
@NoArgsConstructor
public class Rol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true, length = 30)
    private String nombre;

    @Column(name = "descripcion", length = 120)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "rol_modulo",
        joinColumns = @JoinColumn(name = "id_rol"),
        inverseJoinColumns = @JoinColumn(name = "id_modulo")
    )
    private Set<Modulo> modulos = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rol rol = (Rol) o;
        return id != null && id.equals(rol.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}