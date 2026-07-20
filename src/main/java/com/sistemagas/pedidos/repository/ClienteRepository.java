package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    
    Optional<Cliente> findByTelefono(String telefono);

    Optional<Cliente> findByEmail(String email);

    Optional<Cliente> findByDni(String dni);

    Optional<Cliente> findByUuidOffline(String uuidOffline);

    List<Cliente> findByActivoTrue();

    List<Cliente> findByUuidOfflineIn(List<String> uuidsOffline);

    // Puedes agregar más métodos de búsqueda (por nombre, DNI si lo agregas luego, etc.)
}
