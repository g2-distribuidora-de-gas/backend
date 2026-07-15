package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.Ruta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RutaRepository extends JpaRepository<Ruta, Long> {

    // Útil para buscar las rutas de un repartidor para una fecha y estado especificos.
    // Devuelve List porque en produccion puede haber multiples rutas en el mismo estado
    // (ej: varios lotes planificados). El caller toma la primera.
    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente", "repartidor"})
    List<Ruta> findByRepartidorIdAndFechaRepartoAndEstado(Long repartidorId, LocalDate fechaReparto, EstadoRuta estado);

    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente", "repartidor"})
    Optional<Ruta> findById(Long id);

    // Listar todas las rutas de un día en particular (útil para el admin)
    List<Ruta> findByFechaReparto(LocalDate fechaReparto);
    
    // Obtener las rutas activas o en curso de un repartidor
    List<Ruta> findByRepartidorIdAndEstadoIn(Long repartidorId, List<EstadoRuta> estados);
}
