package com.sistemagas.pedidos.enums;

public enum TipoGarrafa {
    GARRAFA_10KG(10),
    GARRAFA_15KG(15),
    GARRAFA_45KG(45);

    private final int capacidadKg;

    TipoGarrafa(int capacidadKg) {
        this.capacidadKg = capacidadKg;
    }

    public int getCapacidadKg() {
        return capacidadKg;
    }
}