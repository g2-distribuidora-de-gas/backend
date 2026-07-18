package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.TipoMovimiento;
import com.sistemagas.pedidos.model.MovimientoGarrafa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MovimientoGarrafaRepository extends JpaRepository<MovimientoGarrafa, Long> {

    /**
     * Historial paginado con filtros opcionales.
     * Los parámetros nulos son ignorados por la condición IS NULL.
     */
    @Query("SELECT m FROM MovimientoGarrafa m " +
           "JOIN FETCH m.tipoGarrafa " +
           "JOIN FETCH m.estadoDestino " +
           "LEFT JOIN FETCH m.estadoOrigen " +
           "LEFT JOIN FETCH m.depositoOrigen " +
           "LEFT JOIN FETCH m.depositoDestino " +
           "JOIN FETCH m.usuario " +
           "WHERE (cast(:depositoId as Long) IS NULL OR m.depositoOrigen.id = :depositoId OR m.depositoDestino.id = :depositoId) " +
           "AND (cast(:tipoId as Long) IS NULL OR m.tipoGarrafa.id = :tipoId) " +
           "AND (cast(:tipoMov as text) IS NULL OR m.tipoMovimiento = :tipoMov) " +
           "AND (cast(:desde as timestamp) IS NULL OR m.fecha >= :desde) " +
           "AND (cast(:hasta as timestamp) IS NULL OR m.fecha <= :hasta) " +
           "ORDER BY m.fecha DESC")
    Page<MovimientoGarrafa> findByFiltros(
        @Param("depositoId") Long depositoId,
        @Param("tipoId") Long tipoId,
        @Param("tipoMov") TipoMovimiento tipoMov,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        Pageable pageable
    );

    /**
     * Todos los movimientos asociados a un pedido específico.
     */
    List<MovimientoGarrafa> findByPedidoIdOrderByFechaDesc(Long pedidoId);
}
