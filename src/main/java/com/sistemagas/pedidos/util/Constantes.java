package com.sistemagas.pedidos.util;

public final class Constantes {

    private Constantes() {
    }

    public static final String API_PEDIDOS = "/api/pedidos";
    public static final String API_SINCRONIZAR = "/api/sincronizar";

    public static final String MSG_PEDIDO_NO_ENCONTRADO = "Pedido no encontrado";
    public static final String MSG_GARRAFA_NO_ENCONTRADA = "Garrafa no encontrada";
    public static final String MSG_USUARIO_NO_ENCONTRADO = "Usuario no encontrado";
    public static final String MSG_PEDIDO_DUPLICADO = "Ya existe un pedido con ese uuidOffline";

    public static final String MSG_SINCRONIZACION_OK = "Sincronizacion procesada";
}