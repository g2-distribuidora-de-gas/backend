package com.sistemagas.pedidos.exception;

import org.springframework.http.HttpStatus;

public class DepositoInactivoException extends BusinessException {
    public DepositoInactivoException(String mensaje) {
        super(mensaje, HttpStatus.CONFLICT, "DEPOSITO_INACTIVO");
    }
}
