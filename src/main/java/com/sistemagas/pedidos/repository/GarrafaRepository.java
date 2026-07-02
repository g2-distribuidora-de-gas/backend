package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.TipoGarrafa;
import com.sistemagas.pedidos.model.Garrafa;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GarrafaRepository extends JpaRepository<Garrafa, Long> {

    boolean existsByTipo(TipoGarrafa tipo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Garrafa g WHERE g.id = :id")
    Optional<Garrafa> findByIdForUpdate(@Param("id") Long id);
}