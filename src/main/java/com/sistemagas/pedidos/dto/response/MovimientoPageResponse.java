package com.sistemagas.pedidos.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MovimientoPageResponse {

    private List<MovimientoResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
