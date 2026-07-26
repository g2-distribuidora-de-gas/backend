package com.sistemagas.pedidos.dto.request;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Payload para registrar o actualizar el cobro de una parada.
 *
 * <p>El {@code estadoPago} <b>no se envía</b>: el backend lo deriva de los montos:
 * <ul>
 *   <li>suma == total → PAGADO</li>
 *   <li>0 &lt; suma &lt; total → PARCIAL</li>
 *   <li>suma == 0 → PENDIENTE (se requiere {@link #motivoPendiente})</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistrarCobroRequest {

    /** Monto cobrado en efectivo. {@code null} se interpreta como 0. */
    @DecimalMin(value = "0.00", message = "El monto en efectivo no puede ser negativo")
    private BigDecimal montoEfectivo;

    /** Monto cobrado por transferencia. {@code null} se interpreta como 0. */
    @DecimalMin(value = "0.00", message = "El monto de transferencia no puede ser negativo")
    private BigDecimal montoTransferencia;

    /**
     * Motivo del pendiente o pago parcial.
     * Obligatorio cuando la suma de montos == 0 (estado PENDIENTE).
     * Opcional para pago parcial.
     * Ejemplos: "Cliente no disponible", "Transferencia en camino", "Pago acordado para mañana".
     */
    private String motivoPendiente;
}
