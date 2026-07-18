package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.TipoGarrafaStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoGarrafaStockRepository extends JpaRepository<TipoGarrafaStock, Long> {

    List<TipoGarrafaStock> findByActivoTrue();

    Optional<TipoGarrafaStock> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
