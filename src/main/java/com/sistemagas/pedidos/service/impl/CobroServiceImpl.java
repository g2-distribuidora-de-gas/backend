package com.sistemagas.pedidos.service.impl;

import com.sistemagas.pedidos.dto.request.RegistrarCobroRequest;
import com.sistemagas.pedidos.dto.response.PagoPedidoResponse;
import com.sistemagas.pedidos.dto.response.ResumenCobroResponse;
import com.sistemagas.pedidos.enums.EstadoEntrega;
import com.sistemagas.pedidos.enums.EstadoPago;
import com.sistemagas.pedidos.exception.BusinessException;
import com.sistemagas.pedidos.exception.ResourceNotFoundException;
import com.sistemagas.pedidos.model.PagoPedido;
import com.sistemagas.pedidos.model.PedidoDetalle;
import com.sistemagas.pedidos.model.RutaPedido;
import com.sistemagas.pedidos.model.TipoGarrafaStock;
import com.sistemagas.pedidos.model.Usuario;
import com.sistemagas.pedidos.repository.PagoPedidoRepository;
import com.sistemagas.pedidos.repository.RutaPedidoRepository;
import com.sistemagas.pedidos.repository.TipoGarrafaStockRepository;
import com.sistemagas.pedidos.service.CobroService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CobroServiceImpl implements CobroService {

    private final RutaPedidoRepository rutaPedidoRepository;
    private final PagoPedidoRepository pagoPedidoRepository;
    private final TipoGarrafaStockRepository tipoGarrafaStockRepository;
    private final com.sistemagas.pedidos.service.SupabaseStorageService supabaseStorageService;

    // ─── Registrar / actualizar cobro ────────────────────────────────────────────

    @Override
    @Transactional
    public PagoPedidoResponse registrarCobro(Long rutaPedidoId,
                                             RegistrarCobroRequest request,
                                             org.springframework.web.multipart.MultipartFile comprobante,
                                             Usuario cobrador) {

        RutaPedido parada = rutaPedidoRepository.findById(rutaPedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parada no encontrada: " + rutaPedidoId));

        // ── Autorización: el REPARTIDOR solo cobra sus propias paradas ────────
        if ("REPARTIDOR".equals(cobrador.getRol().name())) {
            Long duenioId = parada.getRuta().getRepartidor().getId();
            if (!duenioId.equals(cobrador.getId())) {
                throw new BusinessException(
                        "No puedes registrar cobros de paradas de otro repartidor",
                        HttpStatus.FORBIDDEN, "COBRO_NO_AUTORIZADO");
            }
        }

        // ── Paradas FALLIDO no tienen entrega → no se cobran ─────────────────
        if (parada.getEstadoEntrega() == EstadoEntrega.FALLIDO) {
            throw new BusinessException(
                    "No se puede registrar cobro de una parada fallida",
                    HttpStatus.UNPROCESSABLE_ENTITY, "PARADA_FALLIDA");
        }

        // ── Normalizar montos (null → 0) ──────────────────────────────────────
        BigDecimal efectivo      = nvl(request.getMontoEfectivo());
        BigDecimal transferencia = nvl(request.getMontoTransferencia());
        BigDecimal totalPagado   = efectivo.add(transferencia);

        // ── Validar Comprobante ───────────────────────────────────────────────
        if (transferencia.compareTo(BigDecimal.ZERO) > 0) {
            if (comprobante == null || comprobante.isEmpty()) {
                throw new BusinessException(
                        "El comprobante de transferencia es obligatorio",
                        HttpStatus.BAD_REQUEST, "COMPROBANTE_REQUERIDO");
            }
        }

        // ── Calcular total del pedido a partir de lo entregado ────────────────
        BigDecimal totalPedido = calcularTotalEntregado(parada);

        // ── Validar que no se pague de más ────────────────────────────────────
        if (totalPagado.compareTo(totalPedido) > 0) {
            throw new BusinessException(
                    String.format("El monto pagado (%.2f) supera el total del pedido (%.2f)",
                            totalPagado, totalPedido),
                    HttpStatus.UNPROCESSABLE_ENTITY, "MONTO_EXCEDIDO");
        }

        // ── Derivar estado ────────────────────────────────────────────────────
        EstadoPago estadoPago = derivarEstado(totalPagado, totalPedido);

        // Si PAGADO, la parada debe estar ENTREGADO
        if (estadoPago == EstadoPago.PAGADO
                && parada.getEstadoEntrega() != EstadoEntrega.ENTREGADO) {
            throw new BusinessException(
                    "Solo se puede registrar un cobro completo en paradas marcadas como ENTREGADO",
                    HttpStatus.UNPROCESSABLE_ENTITY, "PARADA_NO_ENTREGADA");
        }

        // Motivo obligatorio si PENDIENTE (suma == 0)
        if (estadoPago == EstadoPago.PENDIENTE) {
            String motivo = request.getMotivoPendiente();
            if (motivo == null || motivo.isBlank()) {
                throw new BusinessException(
                        "El motivo es obligatorio cuando no se registra ningún monto",
                        HttpStatus.BAD_REQUEST, "MOTIVO_REQUERIDO");
            }
        }

        BigDecimal saldo = totalPedido.subtract(totalPagado).max(BigDecimal.ZERO);

        // ── Upsert ────────────────────────────────────────────────────────────
        Optional<PagoPedido> existente = pagoPedidoRepository.findByRutaPedidoId(rutaPedidoId);

        if (existente.isPresent() && existente.get().getEstadoPago() == EstadoPago.PAGADO) {
            throw new BusinessException(
                    "El cobro ya fue completado y es inmutable",
                    HttpStatus.CONFLICT, "COBRO_YA_PAGADO");
        }

        PagoPedido pago = existente.orElseGet(() -> PagoPedido.builder()
                .rutaPedido(parada)
                .build());

        pago.setTotalPedido(totalPedido);
        pago.setMontoEfectivo(efectivo);
        pago.setMontoTransferencia(transferencia);
        pago.setSaldoPendiente(saldo);
        pago.setEstadoPago(estadoPago);
        pago.setMotivoPendiente(estadoPago == EstadoPago.PAGADO ? null : request.getMotivoPendiente());
        pago.setCobradoPor(cobrador);

        if (comprobante != null && !comprobante.isEmpty()) {
            String path = supabaseStorageService.subir("cobros/parada-" + rutaPedidoId, comprobante, "Comprobante de pago parada " + rutaPedidoId);
            pago.setUrlComprobante(path);
        }

        PagoPedido guardado = pagoPedidoRepository.save(pago);
        log.info("Cobro [{}] registrado para parada {} por usuario {} — estado: {}",
                guardado.getId(), rutaPedidoId, cobrador.getEmail(), estadoPago);

        return toResponse(guardado);
    }

    // ─── Resumen de cobro ─────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResumenCobroResponse obtenerResumenCobro(Long rutaPedidoId, Usuario autenticado) {

        RutaPedido parada = rutaPedidoRepository.findById(rutaPedidoId)
                .orElseThrow(() -> new ResourceNotFoundException("Parada no encontrada: " + rutaPedidoId));

        if ("REPARTIDOR".equals(autenticado.getRol().name())) {
            if (!parada.getRuta().getRepartidor().getId().equals(autenticado.getId())) {
                throw new BusinessException(
                        "No puedes ver el cobro de paradas de otro repartidor",
                        HttpStatus.FORBIDDEN, "COBRO_NO_AUTORIZADO");
            }
        }

        BigDecimal totalPedido = calcularTotalEntregado(parada);
        List<ResumenCobroResponse.ItemCobroResponse> items = buildItems(parada);

        PagoPedidoResponse pagoExistente = pagoPedidoRepository.findByRutaPedidoId(rutaPedidoId)
                .map(this::toResponse)
                .orElse(null);

        return ResumenCobroResponse.builder()
                .rutaPedidoId(rutaPedidoId)
                .totalPedido(totalPedido)
                .detalles(items)
                .pagoExistente(pagoExistente)
                .build();
    }

    // ─── Listados ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PagoPedidoResponse> listarCobrosPendientes() {
        List<PagoPedido> parciales  = pagoPedidoRepository.findByEstadoPago(EstadoPago.PARCIAL);
        List<PagoPedido> pendientes = pagoPedidoRepository.findByEstadoPago(EstadoPago.PENDIENTE);
        List<PagoPedido> todos = new ArrayList<>(parciales);
        todos.addAll(pendientes);
        return todos.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PagoPedidoResponse> listarMisCobros(Usuario repartidor) {
        return pagoPedidoRepository.findByCobradoPorIdOrderByCreatedAtDesc(repartidor.getId())
                .stream().map(this::toResponse).toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Calcula el total a cobrar sumando {@code precioUnitario × cantidadEntregada}
     * por cada detalle del pedido de la parada.
     *
     * <p>Si la parada es PENDIENTE (aún no entregada), se usa la cantidad solicitada
     * como referencia para mostrarle al repartidor cuánto cobrará.
     */
    private BigDecimal calcularTotalEntregado(RutaPedido parada) {
        List<PedidoDetalle> detalles = parada.getPedido().getDetalles();
        if (detalles == null || detalles.isEmpty()) return BigDecimal.ZERO;

        return detalles.stream()
                .map(d -> {
                    int cantidad = d.getCantidadEntregada() != null && d.getCantidadEntregada() > 0
                            ? d.getCantidadEntregada()
                            : d.getCantidad();
                    return d.getPrecioUnitario().multiply(BigDecimal.valueOf(cantidad));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private List<ResumenCobroResponse.ItemCobroResponse> buildItems(RutaPedido parada) {
        List<PedidoDetalle> detalles = parada.getPedido().getDetalles();
        if (detalles == null) return List.of();

        return detalles.stream().map(d -> {
            String tipoNombre = null;
            Optional<TipoGarrafaStock> tipo = tipoGarrafaStockRepository.findById(d.getTipoGarrafaId());
            if (tipo.isPresent()) tipoNombre = tipo.get().getCodigo();

            int cantEntregada = d.getCantidadEntregada() != null ? d.getCantidadEntregada() : 0;
            BigDecimal subtotalEntregado = d.getPrecioUnitario()
                    .multiply(BigDecimal.valueOf(cantEntregada))
                    .setScale(2, RoundingMode.HALF_UP);

            return ResumenCobroResponse.ItemCobroResponse.builder()
                    .pedidoDetalleId(d.getId())
                    .tipoGarrafaId(d.getTipoGarrafaId())
                    .garrafaTipo(tipoNombre)
                    .cantidadSolicitada(d.getCantidad())
                    .cantidadEntregada(cantEntregada)
                    .precioUnitario(d.getPrecioUnitario())
                    .subtotalHistorico(d.getSubtotal())
                    .subtotalEntregado(subtotalEntregado)
                    .build();
        }).toList();
    }

    /** Deriva el EstadoPago a partir de lo cobrado vs. el total. */
    private EstadoPago derivarEstado(BigDecimal totalPagado, BigDecimal totalPedido) {
        int cmp = totalPagado.compareTo(totalPedido);
        if (cmp >= 0)                            return EstadoPago.PAGADO;
        if (totalPagado.compareTo(BigDecimal.ZERO) > 0) return EstadoPago.PARCIAL;
        return EstadoPago.PENDIENTE;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private PagoPedidoResponse toResponse(PagoPedido p) {
        return PagoPedidoResponse.builder()
                .id(p.getId())
                .rutaPedidoId(p.getRutaPedido().getId())
                .totalPedido(p.getTotalPedido())
                .montoEfectivo(p.getMontoEfectivo())
                .montoTransferencia(p.getMontoTransferencia())
                .saldoPendiente(p.getSaldoPendiente())
                .estadoPago(p.getEstadoPago())
                .motivoPendiente(p.getMotivoPendiente())
                .cobradoPorId(p.getCobradoPor() != null ? p.getCobradoPor().getId() : null)
                .cobradoPorNombre(p.getCobradoPor() != null ? p.getCobradoPor().getNombre() : null)
                .urlComprobante(p.getUrlComprobante() != null ? supabaseStorageService.getSignedUrl(p.getUrlComprobante()) : null)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
