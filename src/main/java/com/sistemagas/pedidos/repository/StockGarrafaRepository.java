package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.Deposito;
import com.sistemagas.pedidos.model.EstadoGarrafa;
import com.sistemagas.pedidos.model.StockGarrafa;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockGarrafaRepository extends JpaRepository<StockGarrafa, Long> {

    /**
     * Consulta de sólo lectura: stock de un depósito específico.
     */
    @Query("SELECT s FROM StockGarrafa s " +
           "JOIN FETCH s.tipoGarrafa tg " +
           "JOIN FETCH s.estadoGarrafa eg " +
           "WHERE s.deposito.id = :depositoId " +
           "ORDER BY tg.codigo, eg.codigo")
    List<StockGarrafa> findByDepositoId(@Param("depositoId") Long depositoId);

    /**
     * Consulta de sólo lectura: stock total de todos los depósitos activos.
     */
    @Query("SELECT s FROM StockGarrafa s " +
           "JOIN FETCH s.tipoGarrafa " +
           "JOIN FETCH s.estadoGarrafa " +
           "WHERE s.deposito.activo = true")
    List<StockGarrafa> findAllByDepositoActivo();

    /**
     * Busca un registro con bloqueo PESSIMISTIC_WRITE.
     * Alternativa al Optimistic Locking para escenarios de alta contención.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockGarrafa s " +
           "WHERE s.deposito = :deposito " +
           "AND s.tipoGarrafa = :tipo " +
           "AND s.estadoGarrafa = :estado")
    Optional<StockGarrafa> findByDepositoTipoEstadoForUpdate(
        @Param("deposito") Deposito deposito,
        @Param("tipo") TipoGarrafaStock tipo,
        @Param("estado") EstadoGarrafa estado
    );

    /**
     * Busca un registro de stock para lectura/modificación con Optimistic Locking
     * (la versión se verifica automáticamente al hacer flush).
     */
    Optional<StockGarrafa> findByDepositoAndTipoGarrafaAndEstadoGarrafa(
        Deposito deposito,
        TipoGarrafaStock tipoGarrafa,
        EstadoGarrafa estadoGarrafa
    );
}
