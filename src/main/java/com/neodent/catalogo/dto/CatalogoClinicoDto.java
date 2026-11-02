package com.neodent.catalogo.dto;

import com.neodent.catalogo.model.TipoCatalogoClinico;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class CatalogoClinicoDto {

    public record Response(
        Integer id,
        TipoCatalogoClinico tipo,
        String nombre,
        String descripcion,
        Boolean activo
    ) {}

    public record CreateRequest(
        @NotNull(message = "El tipo de catálogo es obligatorio")
        TipoCatalogoClinico tipo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre no puede superar los 120 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
        String descripcion,

        Boolean activo
    ) {}

    @Schema(description = "Catálogos clínicos activos agrupados por tipo para uso inmediato en formularios")
    public record CatalogoAgrupadoResponse(
        List<Response> alergias,
        List<Response> patologias,
        List<Response> habitos,
        List<Response> medicamentos
    ) {}
}
