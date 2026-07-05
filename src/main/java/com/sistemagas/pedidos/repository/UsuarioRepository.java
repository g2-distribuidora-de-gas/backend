package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByDni(String dni);
    boolean existsByDni(String dni);

    List<Usuario> findByUpdatedAtGreaterThan(Instant minUpdatedAt, Pageable pageable);
    List<Usuario> findAllBy(Pageable pageable);
}
