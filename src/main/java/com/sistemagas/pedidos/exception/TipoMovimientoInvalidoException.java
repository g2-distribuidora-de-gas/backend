package com.sistemagas.pedidos.exception;

import org.springframework.http.HttpStatus;

public class TipoMovimientoInvalidoException extends BusinessException {
    public TipoMovimientoInvalidoException(String mensaje) {
        super(mensaje, HttpStatus.BAD_REQUEST, "TIPO_MOVIMIENTO_INVALIDO");
    }
}
