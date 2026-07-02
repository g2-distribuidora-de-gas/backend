package com.sistemagas.pedidos.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public BusinessException(String mensaje) {
        super(mensaje);
        this.status = HttpStatus.BAD_REQUEST;
        this.codigo = "BUSINESS_ERROR";
    }

    public BusinessException(String mensaje, HttpStatus status, String codigo) {
        super(mensaje);
        this.status = status;
        this.codigo = codigo;
    }
}