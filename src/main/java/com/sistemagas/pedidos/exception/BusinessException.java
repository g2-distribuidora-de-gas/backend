package com.sistemagas.pedidos.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Schema(description = "Excepcion de negocio que se traduce a una respuesta HTTP con codigo " +
        "estructurado via ApiResponse.data.codigo. Los codigos conocidos son: USUARIO_NO_ENCONTRADO, " +
        "NO_AUTHENTICATED, GARRAFA_TIPO_OBLIGATORIO, GARRAFA_CAPACIDAD_OBLIGATORIA, " +
        "GARRAFA_CAPACIDAD_INCONSISTENTE, PEDIDO_DUPLICADO, BUSINESS_ERROR.")
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