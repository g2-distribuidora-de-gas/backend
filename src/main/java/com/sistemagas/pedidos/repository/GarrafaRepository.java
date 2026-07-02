package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.model.Garrafa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GarrafaRepository extends JpaRepository<Garrafa, Long> {
    boolean existsByTipo(TipoGarrafa tipo);
}
