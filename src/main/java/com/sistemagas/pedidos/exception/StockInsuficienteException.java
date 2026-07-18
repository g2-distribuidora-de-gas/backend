package com.sistemagas.pedidos.exception;

/**
 * Lanzada cuando se intenta decrementar stock por debajo de cero.
 * Se traduce a HTTP 409 Conflict en el manejador global de excepciones.
 */
public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(String message) {
        super(message);
    }
}
