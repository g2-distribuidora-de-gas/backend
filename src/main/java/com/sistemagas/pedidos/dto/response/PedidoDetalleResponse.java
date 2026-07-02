package com.sistemagas.pedidos.dto.response;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoDetalleResponse {

    private Long id;
    private Long garrafaId;
    private TipoGarrafa garrafaTipo;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;
}