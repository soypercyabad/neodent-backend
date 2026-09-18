package com.neodent.shared.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginaResponse<T>(
    List<T> contenido,
    int pagina,
    int tamanoPagina,
    long totalElementos,
    int totalPaginas,
    boolean esPrimera,
    boolean esUltima
) {

    public static <T> PaginaResponse<T> de(Page<T> page) {
        return new PaginaResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
