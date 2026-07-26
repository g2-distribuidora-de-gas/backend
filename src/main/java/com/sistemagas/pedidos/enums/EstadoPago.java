package com.sistemagas.pedidos.enums;

/**
 * Estado financiero de un cobro registrado por el repartidor.
 *
 * <p>El estado se <b>deriva automáticamente</b> de los montos pagados:
 * <ul>
 *   <li>{@link #PAGADO}   — efectivo + transferencia == totalPedido</li>
 *   <li>{@link #PARCIAL}  — 0 < suma < totalPedido</li>
 *   <li>{@link #PENDIENTE}— suma == 0 (no cobró nada; requiere motivoPendiente)</li>
 * </ul>
 *
 * <p>Solo {@link #PARCIAL} y {@link #PENDIENTE} son editables.
 * Un cobro {@link #PAGADO} es inmutable.
 */
public enum EstadoPago {
    PAGADO,
    PARCIAL,
    PENDIENTE
}
