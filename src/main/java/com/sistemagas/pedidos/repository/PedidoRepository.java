package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import java.time.Instant;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Optional<Pedido> findByUuidOffline(String uuidOffline);

    boolean existsByUuidOffline(String uuidOffline);

    List<Pedido> findByUuidOfflineIn(List<String> uuids);

    List<Pedido> findByUpdatedAtGreaterThan(Instant minUpdatedAt, Pageable pageable);
    List<Pedido> findAllBy(Pageable pageable);

    List<Pedido> findByCreadorIdAndUpdatedAtGreaterThan(Long creadorId, Instant minUpdatedAt, Pageable pageable);
    List<Pedido> findByCreadorId(Long creadorId, Pageable pageable);
}