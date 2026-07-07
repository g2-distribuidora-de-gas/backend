package com.sistemagas.pedidos.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoFotoResponse {

    private Long pedidoId;
    private String urlFotoEvidencia;
    private String nombreArchivo;
    private String contentType;
    private Long tamanioBytes;
}