package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.EstadoGarrafa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstadoGarrafaRepository extends JpaRepository<EstadoGarrafa, Long> {

    List<EstadoGarrafa> findByActivoTrue();

    Optional<EstadoGarrafa> findByCodigo(String codigo);
}
