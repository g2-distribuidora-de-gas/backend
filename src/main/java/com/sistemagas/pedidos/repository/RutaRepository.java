package com.sistemagas.pedidos.repository;

import com.sistemagas.pedidos.enums.EstadoRuta;
import com.sistemagas.pedidos.model.Ruta;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.time.Instant;
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

    // Variante con varios estados para soportar la consulta priorizada de ruta activa
    // (EN_CURSO > PLANIFICADA > REPROGRAMADA). Devuelve List para mantener consistencia.
    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente", "repartidor"})
    List<Ruta> findByRepartidorIdAndFechaRepartoAndEstadoIn(Long repartidorId, LocalDate fechaReparto, List<EstadoRuta> estados);

    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente", "repartidor"})
    Optional<Ruta> findById(Long id);

    // Listar todas las rutas de un día en particular (útil para el admin)
    List<Ruta> findByFechaReparto(LocalDate fechaReparto);

    // Obtener las rutas activas o en curso de un repartidor
    List<Ruta> findByRepartidorIdAndEstadoIn(Long repartidorId, List<EstadoRuta> estados);

    // Reporte: rutas en un estado y rango de fechas, paginadas.
    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente",
            "paradas.pedido.detalles", "repartidor"})
    List<Ruta> findByEstadoAndFechaRepartoBetween(EstadoRuta estado, LocalDate desde, LocalDate hasta, Pageable pageable);

    // Reporte: rutas de un repartidor en un estado y rango de fechas, paginadas.
    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente",
            "paradas.pedido.detalles", "repartidor"})
    List<Ruta> findByRepartidorIdAndEstadoAndFechaRepartoBetween(Long repartidorId, EstadoRuta estado, LocalDate desde, LocalDate hasta, Pageable pageable);

    // Reporte: rutas en un estado con updatedAt mayor a un instante, paginadas.
    @EntityGraph(attributePaths = {"paradas", "paradas.pedido", "paradas.pedido.cliente",
            "paradas.pedido.detalles", "repartidor"})
    List<Ruta> findByEstadoAndUpdatedAtGreaterThan(EstadoRuta estado, Instant minUpdatedAt, Pageable pageable);
}
