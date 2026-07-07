package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.RutaPedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RutaPedidoRepository extends JpaRepository<RutaPedido, Long> {

    // Buscar una parada específica por el ID del pedido (útil si se cancela un pedido externo)
    Optional<RutaPedido> findByPedidoId(Long pedidoId);

}
