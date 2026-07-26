package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.EstadoPago;
import com.sistemagas.pedidos.model.PagoPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagoPedidoRepository extends JpaRepository<PagoPedido, Long> {

    Optional<PagoPedido> findByRutaPedidoId(Long rutaPedidoId);

    List<PagoPedido> findByEstadoPago(EstadoPago estadoPago);

    List<PagoPedido> findByCobradoPorId(Long usuarioId);

    /** Todos los cobros de un repartidor en particular. */
    List<PagoPedido> findByCobradoPorIdOrderByCreatedAtDesc(Long repartidorId);
}
