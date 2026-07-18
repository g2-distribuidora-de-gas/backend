package com.sistemagas.pedidos.dto.request;

import com.sistemagas.pedidos.enums.TipoDeposito;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepositoRequest {

    @NotBlank(message = "El nombre del depósito es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotNull(message = "El tipo de depósito es obligatorio")
    private TipoDeposito tipo;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String descripcion;

    @Size(max = 20, message = "La patente no puede superar los 20 caracteres")
    private String vehiculoPatente;

    private Long repartidorId;
}
