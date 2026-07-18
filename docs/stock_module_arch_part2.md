# Módulo de Stock — Arquitectura Técnica (Parte 2)

## 6. Flujo de cada operación

### UC-11: Cargar camión
1. El despachante envía `POST /stock/camion/{camionId}/carga`
2. `TransferenciaService.cargarCamion()` recibe el request y el usuario autenticado
3. Valida que el camión exista y esté activo
4. Valida que el depósito central exista y esté activo
5. Valida que la cantidad > 0
6. Llama a `MovimientoStockService.decrementarStock(depositoCentral, tipo, LLENA, cantidad)` — verifica disponibilidad, lanza excepción si stock insuficiente
7. Llama a `MovimientoStockService.incrementarStock(camion, tipo, LLENA, cantidad)`
8. Persiste `MovimientoGarrafa` con tipo=CARGA_CAMION, origen=depositoCentral, destino=camion
9. Retorna `MovimientoResponse`

### UC-12: Descargar camión
1. `POST /stock/camion/{camionId}/descarga`
2. Igual que carga pero invierte origen/destino
3. Transfiere LLENAS restantes y VACIAS acumuladas de regreso al depósito central
4. Puede incluir múltiples ítems (llenas + vacías) en una sola petición → una transacción, múltiples movimientos

### UC-13: Transferir stock entre depósitos
1. `POST /movimientos/transferencia`
2. `TransferenciaService.transferir()` valida origen ≠ destino
3. Valida disponibilidad en origen
4. Decrementa origen, incrementa destino
5. Registra movimiento con tipo=TRANSFERENCIA
6. Transacción única `@Transactional`

### UC-14: Registrar venta
1. El repartidor envía `POST /movimientos/venta`
2. `InventarioService.registrarVenta()` recibe `VentaStockRequest` (camionId, tipoId, cantidadLlenas, cantidadVacias)
3. Valida que el camión exista y sea de tipo CAMION
4. Valida que el camión tenga suficientes garrafas LLENAS
5. Decrementa stock LLENA del camión en `cantidadLlenas`
6. Incrementa stock VACIA del camión en `cantidadVacias` (las que recibe el repartidor del cliente)
7. Registra movimiento tipo=VENTA con pedido_id si aplica
8. Retorna resumen del estado del camión post-venta

### UC-15: Registrar devolución
1. `POST /movimientos/devolucion`
2. El cliente devuelve una garrafa al repartidor o directamente al depósito
3. Incrementa stock del depósito destino en el estado correspondiente (LLENA o VACIA)
4. Registra movimiento tipo=DEVOLUCION
5. Referencia opcional al pedido original

### UC-16: Registrar rotura
1. `POST /movimientos/rotura`
2. Valida que el depósito tenga la cantidad indicada
3. Decrementa stock del depósito
4. Registra movimiento tipo=ROTURA con destino=null (baja definitiva)
5. Requiere observaciones obligatorias

### UC-17: Iniciar reparación
1. `POST /movimientos/reparacion/iniciar`
2. Decrementa stock en estado VACIA (o LLENA si aplica)
3. Incrementa stock en estado REPARACION del mismo depósito (taller)
4. Registra movimiento tipo=REPARACION con estado_origen → REPARACION

### UC-18: Finalizar reparación
1. `POST /movimientos/reparacion/finalizar`
2. Decrementa stock REPARACION del taller
3. Incrementa stock LLENA (reparada exitosamente) o FUERA_SERVICIO (irreparable)
4. Registra movimiento con estado_destino = LLENA | FUERA_SERVICIO

### UC-19/20: Ajuste de inventario
1. `POST /movimientos/ajuste`
2. Solo accesible para roles ADMIN/SUPERVISOR
3. Entrada: incrementa sin origen (origen=null)
4. Salida: decrementa sin destino (destino=null)
5. Observaciones obligatorias
6. Registra movimiento tipo=AJUSTE_ENTRADA | AJUSTE_SALIDA

### UC-21/22/23: Consultas de stock
- Consultas de sólo lectura, sin `@Transactional` de escritura
- `GET /stock/deposito/{id}` → agrupado por tipo + estado
- `GET /stock/camion/{id}` → mismo formato, verificando tipo=CAMION
- `GET /stock/total` → suma de todos los depósitos, agrupado

### UC-24/25/26: Historial de movimientos
- `GET /movimientos?depositoId=&tipoId=&desde=&hasta=&page=&size=`
- Paginado con Spring Pageable
- Sin límite de historial (registro inmutable)

---

## 7. Reglas de negocio

| # | Regla |
|---|---|
| RN-01 | El stock de un depósito nunca puede ser negativo. Toda operación que llevaría a cantidad < 0 debe lanzar `StockInsuficienteException` |
| RN-02 | No puede existir un movimiento sin registro en `movimientos_garrafa`. Toda modificación de stock debe acompañarse de su movimiento |
| RN-03 | El depósito origen y destino deben existir y estar activos al momento del movimiento |
| RN-04 | El tipo de garrafa debe existir y estar activo |
| RN-05 | El estado de garrafa debe ser válido para la operación (no se puede vender una VACIA) |
| RN-06 | En una venta: solo se pueden entregar garrafas en estado LLENA |
| RN-07 | En una venta: las garrafas recibidas del cliente ingresan como VACIA |
| RN-08 | No se puede transferir desde y hacia el mismo depósito |
| RN-09 | La cantidad de cualquier operación debe ser > 0 |
| RN-10 | Solo ADMIN/SUPERVISOR pueden realizar ajustes manuales de inventario |
| RN-11 | Solo ADMIN/SUPERVISOR pueden registrar roturas |
| RN-12 | Los movimientos de reparación solo pueden realizarse hacia/desde un depósito de tipo TALLER |
| RN-13 | La carga de un camión solo puede tener como origen un DEPOSITO_CENTRAL o PLANTA |
| RN-14 | Toda operación debe registrar el usuario que la realizó (auditoría) |
| RN-15 | Las observaciones son obligatorias en ajustes y roturas |
| RN-16 | Un camión no puede ser destino de una venta (las ventas salen del camión) |
| RN-17 | El stock total = suma de stock_garrafa de todos los depósitos activos |

---

## 8. Diseño de APIs REST

### Depósitos
```
POST   /api/v1/depositos                       → Crear depósito
GET    /api/v1/depositos                       → Listar depósitos (con filtro por tipo)
GET    /api/v1/depositos/{id}                  → Obtener por ID
PUT    /api/v1/depositos/{id}                  → Editar depósito
PATCH  /api/v1/depositos/{id}/estado           → Activar/Desactivar
```

### Tipos y estados de garrafa
```
POST   /api/v1/tipos-garrafa                   → Crear tipo
GET    /api/v1/tipos-garrafa                   → Listar tipos
PUT    /api/v1/tipos-garrafa/{id}              → Editar tipo
GET    /api/v1/estados-garrafa                 → Listar estados
```

### Stock (consultas)
```
GET    /api/v1/stock/deposito/{id}             → Stock de un depósito
GET    /api/v1/stock/camion/{id}               → Stock de un camión
GET    /api/v1/stock/total                     → Stock total del sistema
```

### Movimientos (operaciones)
```
POST   /api/v1/movimientos/transferencia       → Transferencia entre depósitos
POST   /api/v1/movimientos/venta               → Registrar venta del repartidor
POST   /api/v1/movimientos/devolucion          → Registrar devolución
POST   /api/v1/movimientos/rotura              → Registrar rotura
POST   /api/v1/movimientos/ajuste              → Ajuste manual de inventario
POST   /api/v1/movimientos/reparacion/iniciar  → Iniciar reparación
POST   /api/v1/movimientos/reparacion/finalizar→ Finalizar reparación
POST   /api/v1/camiones/{id}/carga             → Cargar camión desde depósito
POST   /api/v1/camiones/{id}/descarga          → Descargar camión al depósito
```

### Historial
```
GET    /api/v1/movimientos                     → Historial paginado (filtros: depositoId, tipoId, desde, hasta, tipoMovimiento)
GET    /api/v1/movimientos/{id}                → Detalle de un movimiento
GET    /api/v1/movimientos/pedido/{pedidoId}   → Movimientos de un pedido
```

### Ejemplos de request/response

#### POST /api/v1/movimientos/venta
```json
// Request
{
  "camionId": 5,
  "tipoGarrafaId": 1,
  "cantidadEntregadas": 1,
  "cantidadRecibidas": 1,
  "pedidoId": 42,
  "observaciones": "Entrega normal"
}

// Response
{
  "id": 101,
  "tipoMovimiento": "VENTA",
  "depositoOrigen": { "id": 5, "nombre": "Camión 01 - Juan", "tipo": "CAMION" },
  "depositoDestino": null,
  "tipoGarrafa": { "id": 1, "codigo": "10KG", "descripcion": "Garrafa 10 Kg" },
  "estadoOrigen": "LLENA",
  "estadoDestino": "VACIA",
  "cantidad": 1,
  "pedidoId": 42,
  "usuario": "juan.repartidor@sistemagas.com",
  "fecha": "2026-07-17T20:00:00",
  "observaciones": "Entrega normal"
}
```

#### POST /api/v1/movimientos/transferencia
```json
// Request
{
  "depositoOrigenId": 1,
  "depositoDestinoId": 5,
  "tipoGarrafaId": 1,
  "estadoGarrafaId": 1,
  "cantidad": 20,
  "observaciones": "Carga inicial del día"
}
```

#### GET /api/v1/stock/deposito/1
```json
// Response
{
  "deposito": { "id": 1, "nombre": "Depósito Central", "tipo": "DEPOSITO_CENTRAL" },
  "stock": [
    { "tipoGarrafa": "10KG", "estado": "LLENA", "cantidad": 120 },
    { "tipoGarrafa": "10KG", "estado": "VACIA", "cantidad": 45 },
    { "tipoGarrafa": "15KG", "estado": "LLENA", "cantidad": 60 }
  ],
  "consultadoEn": "2026-07-17T20:00:00"
}
```

#### GET /api/v1/stock/total
```json
// Response
{
  "stock": [
    { "tipoGarrafa": "10KG", "estado": "LLENA", "cantidad": 300 },
    { "tipoGarrafa": "10KG", "estado": "VACIA", "cantidad": 120 },
    { "tipoGarrafa": "15KG", "estado": "LLENA", "cantidad": 150 }
  ],
  "depositosIncluidos": 8,
  "consultadoEn": "2026-07-17T20:00:00"
}
```

---

## 9. Diseño de DTOs

### Request DTOs

| DTO | Campos |
|---|---|
| `DepositoRequest` | nombre, tipo (TipoDeposito), descripcion, vehiculoPatente?, repartidorId? |
| `TransferenciaRequest` | depositoOrigenId, depositoDestinoId, tipoGarrafaId, estadoGarrafaId, cantidad, observaciones? |
| `VentaStockRequest` | camionId, tipoGarrafaId, cantidadEntregadas, cantidadRecibidas, pedidoId?, observaciones? |
| `DevolucionRequest` | depositoDestinoId, tipoGarrafaId, estadoGarrafaId, cantidad, pedidoId?, observaciones? |
| `CargaCamionRequest` | camionId, depositoCentralId, items: [{tipoGarrafaId, cantidad}], observaciones? |
| `DescargaCamionRequest` | camionId, depositoCentralId, items: [{tipoGarrafaId, estadoGarrafaId, cantidad}], observaciones? |
| `AjusteInventarioRequest` | depositoId, tipoGarrafaId, estadoGarrafaId, cantidad, tipoAjuste (ENTRADA/SALIDA), observaciones |
| `RoturaRequest` | depositoId, tipoGarrafaId, cantidad, observaciones |
| `ReparacionInicioRequest` | depositoOrigenId, tallerDestinoId, tipoGarrafaId, estadoOrigenId, cantidad, observaciones? |
| `ReparacionFinRequest` | tallerId, tipoGarrafaId, cantidad, estadoFinal (LLENA/FUERA_SERVICIO), observaciones? |

### Response DTOs

| DTO | Campos |
|---|---|
| `DepositoResponse` | id, nombre, tipo, descripcion, activo, vehiculoPatente?, repartidor? |
| `StockGarrafaResponse` | tipoGarrafa, estado, cantidad |
| `StockDepositoResponse` | deposito, stock: [StockGarrafaResponse], consultadoEn |
| `StockTotalResponse` | stock: [StockGarrafaResponse], depositosIncluidos, consultadoEn |
| `MovimientoResponse` | id, tipoMovimiento, depositoOrigen?, depositoDestino?, tipoGarrafa, estadoOrigen?, estadoDestino, cantidad, pedidoId?, usuario, fecha, observaciones? |
| `MovimientoPageResponse` | content: [MovimientoResponse], page, size, totalElements, totalPages |
