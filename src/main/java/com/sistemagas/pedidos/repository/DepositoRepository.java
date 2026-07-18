package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.TipoDeposito;
import com.sistemagas.pedidos.model.Deposito;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepositoRepository extends JpaRepository<Deposito, Long> {

    List<Deposito> findByActivoTrue();

    List<Deposito> findByTipoAndActivoTrue(TipoDeposito tipo);
    
    java.util.Optional<Deposito> findByRepartidorIdAndActivoTrue(Long repartidorId);

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
