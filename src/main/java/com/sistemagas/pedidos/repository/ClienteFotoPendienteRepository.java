package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.ClienteFotoPendiente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteFotoPendienteRepository extends JpaRepository<ClienteFotoPendiente, Long> {

    Optional<ClienteFotoPendiente> findFirstByClienteIdOrderByUploadedAtDesc(Long clienteId);

    void deleteByClienteId(Long clienteId);

    List<ClienteFotoPendiente> findByUploadedAtBefore(Instant cutoff);
}
