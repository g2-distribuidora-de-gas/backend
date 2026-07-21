package com.sistemagas.pedidos.enums;

public enum ConfirmacionRepartidor {
    /** Turno recién asignado, el repartidor aún no respondió. */
    PENDIENTE,
    /** El repartidor aceptó el turno. */
    CONFIRMADO,
    /** El repartidor rechazó el turno (ver motivoRechazo en Ruta). */
    RECHAZADO
}
