package com.neodent.usuario.repository;

import com.neodent.usuario.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"roles", "estado"})
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    @EntityGraph(attributePaths = {"roles", "estado"})
    Optional<Usuario> findConRolesById(Long id);

    boolean existsByCorreoIgnoreCase(String correo);

    boolean existsByCorreoIgnoreCaseAndIdNot(String correo, Long id);

    boolean existsByAliasInternoIgnoreCase(String aliasInterno);

    @Query(
        value = """
            SELECT u
            FROM Personal p
            JOIN p.usuario u
            WHERE EXISTS (
                SELECT 1
                FROM Usuario u2
                JOIN u2.roles r
                WHERE u2.id = u.id
                AND r.nombre IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO')
                AND (:rol IS NULL OR r.nombre = :rol)
            )
            AND (:estado IS NULL OR u.estado.nombre = :estado)
            AND (:activo IS NULL OR p.activo = :activo)
            AND (
                    :buscar IS NULL
                    OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(u.aliasInterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', :buscar, '%'))
            )
            """,
        countQuery = """
            SELECT COUNT(u)
            FROM Personal p
            JOIN p.usuario u
            WHERE EXISTS (
                SELECT 1
                FROM Usuario u2
                JOIN u2.roles r
                WHERE u2.id = u.id
                AND r.nombre IN ('ADMIN', 'RECEPCIONISTA', 'ODONTOLOGO')
                AND (:rol IS NULL OR r.nombre = :rol)
            )
            AND (:estado IS NULL OR u.estado.nombre = :estado)
            AND (:activo IS NULL OR p.activo = :activo)
            AND (
                    :buscar IS NULL
                    OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(u.aliasInterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.apellidoPaterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.apellidoMaterno) LIKE LOWER(CONCAT('%', :buscar, '%'))
                    OR LOWER(p.numeroDocumento) LIKE LOWER(CONCAT('%', :buscar, '%'))
            )
            """
    )
    Page<Usuario> buscarUsuariosInternos(
        @Param("rol") String rol,
        @Param("estado") String estado,
        @Param("activo") Boolean activo,
        @Param("buscar") String buscar,
        Pageable pageable
    );
}