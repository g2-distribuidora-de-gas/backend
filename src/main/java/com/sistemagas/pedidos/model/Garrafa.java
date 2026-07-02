package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.TipoGarrafa;

import java.math.BigDecimal;

/**
 * Interfaz del modelo Garrafa. Dev 1 (pedidos) la define con los metodos que necesita.
 * Dev 2 (catalog) la implementa con su entidad JPA real.
 */
public interface Garrafa {

    Long getId();

    Integer getStockDisponible();

    void setStockDisponible(Integer stockDisponible);

    BigDecimal getPrecio();

    TipoGarrafa getTipo();
}