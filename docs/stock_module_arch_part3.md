# Módulo de Stock — Arquitectura Técnica (Parte 3)

## 10. Transacciones

| Operación | @Transactional | Por qué |
|---|---|---|
| Cargar camión | ✅ | Decrementa depósito central + incrementa camión + registra movimiento. Si falla cualquier paso, todo revierte |
| Descargar camión | ✅ | Ídem, múltiples actualizaciones de stock en una sola unidad de trabajo |
| Transferencia | ✅ | Misma razón: dos escrituras en `stock_garrafa` + un movimiento deben ser atómicas |
| Registrar venta | ✅ | Decremento LLENA + incremento VACIA + movimiento: atómico |
| Registrar rotura | ✅ | Decremento stock + movimiento: atómico |
| Ajuste inventario | ✅ | Escritura de stock + movimiento: atómico |
| Iniciar/Finalizar reparación | ✅ | Cambio de estado entre depósitos: atómico |
| Consultar stock | ❌ (readOnly) | Solo lectura. Usar `@Transactional(readOnly = true)` para optimización |
| Consultar historial | ❌ (readOnly) | Solo lectura |
| Crear depósito | ✅ (simple) | Una sola entidad, pero buena práctica mantenerlo transaccional |

**Regla general:** Toda operación que escriba en `stock_garrafa` Y en `movimientos_garrafa` simultáneamente es OBLIGATORIAMENTE transaccional. No existe excepción.

**Propagación**: usar `Propagation.REQUIRED` (default). `MovimientoStockService` puede ser llamado desde `TransferenciaService` e `InventarioService` participando en la misma transacción padre.

---

## 11. Concurrencia

### Problema
Dos repartidores del mismo camión (o dos despachantes) operan simultáneamente sobre el mismo registro de `stock_garrafa`. Sin control, uno sobrescribe al otro.

### Solución elegida: Optimistic Locking + Retry selectivo

**Por qué Optimistic Locking:**
- El conflicto real en este dominio es bajo. Los camiones son asignados a un repartidor. Depósitos centrales sí pueden tener más concurrencia pero los lotes son rápidos.
- No requiere mantener locks a nivel de BD durante toda la transacción (más eficiente que Pessimistic)
- Spring Data JPA lo soporta nativamente con `@Version`
- Implementación simple y trazable

**Implementación:**
```
@Version
private Long version;  // ya presente en la entidad StockGarrafa
```

Spring lanza `ObjectOptimisticLockingFailureException` si detecta una versión desactualizada al hacer flush.

**Retry para casos de alta concurrencia (depósito central):**
Para el depósito central —que sí puede tener múltiples cargas simultáneas— se añade un mecanismo de retry usando `@Retryable` de Spring Retry:

```java
@Retryable(
  retryFor = ObjectOptimisticLockingFailureException.class,
  maxAttempts = 3,
  backoff = @Backoff(delay = 100, multiplier = 2)
)
public MovimientoResponse transferir(TransferenciaRequest req, Usuario usuario) { ... }
```

**Cuándo considerar Pessimistic Locking:**
- Si en el futuro se detectan contenciones frecuentes en el depósito central con carga real
- Usar `@Lock(LockModeType.PESSIMISTIC_WRITE)` en el método repository para `findByDepositoAndTipoAndEstado`

**Resumen de decisión:**
| Escenario | Estrategia |
|---|---|
| Camión (1 repartidor) | Optimistic Lock (bajo conflicto) |
| Depósito central (múltiples despachos) | Optimistic Lock + Retry (3 intentos) |
| Ajuste manual de inventario | Optimistic Lock (operación humana, baja concurrencia) |

---

## 12. Eventos futuros (Extensibilidad)

### Diseño ya preparado para:

**Múltiples sucursales y empresas**
- La tabla `depositos` ya soporta tipos SUCURSAL y PLANTA
- Agregar columna `empresa_id` en el futuro sin romper el modelo existente

**Número de serie por cilindro (trazabilidad individual)**
- Agregar tabla `cilindros` con columnas: id, numero_serie, tipo_garrafa_id, estado_actual, deposito_actual
- Agregar `cilindro_id` como FK opcional en `movimientos_garrafa`
- El modelo actual no impide esto, sólo gestiona cantidades

**Vencimiento individual**
- Columna `fecha_vencimiento` en la tabla `cilindros`
- Job programado para detectar y marcar FUERA_SERVICIO automáticamente

**Códigos QR y lectores**
- Cada `cilindro` tendría un `codigo_qr` único
- La API REST ya está diseñada para recibir movimientos; solo se añadiría el campo `codigoQr` al request

**Funcionamiento offline + sincronización**
- El sistema ya tiene precedente en el módulo de rutas (`SincronizacionService`)
- Los movimientos de stock también pueden encolarse localmente y sincronizarse
- Agregar campo `sincronizado` y `fecha_sincronizacion` en `movimientos_garrafa`
- Idempotency key en cada request para evitar duplicados

**Spring Events (preparación para desacoplamiento)**
- Definir `MovimientoRegistradoEvent` con los datos del movimiento
- Publicar vía `ApplicationEventPublisher` al final de cada operación
- Listeners futuros: notificaciones, alertas de stock bajo, analytics, auditoría externa

---

## 13. Roadmap de implementación

### Fase 1 — Base de datos y entidades (1-2 días)
- [ ] Migraciones Flyway: `depositos`, `tipos_garrafa_stock`, `estados_garrafa`, `stock_garrafa`, `movimientos_garrafa`
- [ ] Entidades JPA: `Deposito`, `TipoGarrafaStock`, `EstadoGarrafa`, `StockGarrafa`, `MovimientoGarrafa`
- [ ] Enums: `TipoDeposito`, `TipoMovimiento`
- [ ] Repositories básicos
- [ ] Datos iniciales (seed): estados LLENA, VACIA, etc. Depósito central por defecto

### Fase 2 — CRUD de configuración (1 día)
- [ ] `DepositoService` + `DepositoController`
- [ ] `TipoGarrafaStockController`
- [ ] DTOs y Mappers correspondientes
- [ ] Endpoints GET/POST/PUT de depósitos y tipos

### Fase 3 — Motor de stock (2-3 días)
- [ ] `MovimientoStockService` con `decrementarStock` e `incrementarStock`
- [ ] `StockService` con consultas
- [ ] `TransferenciaService`: transferencia, carga camión, descarga camión
- [ ] `InventarioService`: venta, devolución, rotura, ajuste
- [ ] `MovimientoController` con todos los endpoints de escritura
- [ ] `StockController` con endpoints de consulta
- [ ] Optimistic Locking validado con `@Version`

### Fase 4 — Validaciones y seguridad (1 día)
- [ ] Excepciones de negocio: `StockInsuficienteException`, `DepositoInactivoException`, `TipoMovimientoInvalidoException`
- [ ] Validaciones de roles por endpoint
- [ ] Retry con Spring Retry para operaciones concurrentes
- [ ] Tests unitarios de servicios críticos

### Fase 5 — Consultas avanzadas (1-2 días)
- [ ] Historial paginado con filtros dinámicos (Specification o JPQL)
- [ ] Reporte de stock total agrupado
- [ ] Endpoint de movimientos por pedido
- [ ] Integración con módulo de pedidos existente (registrar venta desde pedido)

### Fase 6 — Integración completa (1 día)
- [ ] Vincular `InventarioService.registrarVenta()` al flujo de `PedidoService`
- [ ] Al marcar un pedido como ENTREGADO → disparar venta de stock automáticamente
- [ ] Al marcar DEVOLUCION → disparar devolución de stock

### Fase 7 — Preparación para escala (futuro)
- [ ] Tabla `cilindros` para trazabilidad individual
- [ ] Soporte multi-empresa (`empresa_id`)
- [ ] Spring Events para desacoplamiento
- [ ] Soporte offline con idempotency keys
- [ ] Alertas automáticas de stock mínimo

---

## 14. Notas de integración con el codebase actual

### Relación con `Garrafa` existente
- La entidad `Garrafa` actual tiene `stockDisponible` como campo propio. **Este campo quedará obsoleto** con el nuevo módulo.
- Migración: el stock de `Garrafa` se migra a registros en `stock_garrafa` (depósito central, tipo correspondiente, estado LLENA).
- El campo `tipo` de tipo `TipoGarrafa` (enum) se puede mapear a la nueva tabla `tipos_garrafa_stock` por `codigo`.

### Relación con `Pedido` y `PedidoDetalle`
- Al completar una entrega, el flujo actual en `RutaService` / `PedidoService` puede disparar `InventarioService.registrarVenta()`.
- El `pedido_id` ya está previsto como FK opcional en `movimientos_garrafa`.

### Nomenclatura de paquetes
- Mantener el paquete base `com.sistemagas.pedidos` y crear subpaquete `stock`:
  `com.sistemagas.pedidos.stock.controller`, `com.sistemagas.pedidos.stock.service`, etc.

### Auditoría
- Extender `Auditable` existente para `Deposito`, `StockGarrafa`
- `MovimientoGarrafa` es inmutable (no se edita, no se borra): no necesita `@PreUpdate`
