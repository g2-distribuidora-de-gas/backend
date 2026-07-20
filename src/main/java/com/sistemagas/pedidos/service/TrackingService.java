package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.realtime.EventoRutaWsDto;
import com.sistemagas.pedidos.dto.realtime.PosicionRepartidorDto;
import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.Ruta;
import com.sistemagas.pedidos.model.Usuario;

public interface TrackingService {

    /**
     * Valida, autoriza y retransmite una posicion GPS del repartidor al topico publico
     * /topic/rutas/{rutaId}/posiciones.
     *
     * @throws com.sistemagas.pedidos.exception.BusinessException si la ruta no existe,
     *         no pertenece al repartidor, o no esta en un estado que admita tracking.
     */
    void publicarPosicion(Usuario repartidor, PosicionRepartidorDto dto);

    /**
     * Emite un evento de cambio de estado/parada a /topic/rutas/{rutaId}/eventos.
     * Es la senial que consume el panel admin para refrescar la UI sin polling.
     */
    void emitirEventoRuta(Ruta ruta, EstadoRuta estadoAnterior, EventoRutaWsDto evento);
}
