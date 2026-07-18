# Módulo de Stock — Arquitectura Técnica (Parte 1)

## 1. Arquitectura del módulo

### Paquete base
`com.sistemagas.pedidos.stock`

### Subpaquetes y clases

```
stock/
├── controller/
│   ├── DepositoController
│   ├── TipoGarrafaStockController
│   ├── StockController
│   └── MovimientoController
├── service/
│   ├── DepositoService           (interfaz)
│   ├── StockService              (interfaz)
│   ├── MovimientoStockService    (interfaz)
│   ├── TransferenciaService      (interfaz)
│   ├── InventarioService         (interfaz)
│   └── impl/
│       ├── DepositoServiceImpl
│       ├── StockServiceImpl
│       ├── MovimientoStockServiceImpl
│       ├── TransferenciaServiceImpl
│       └── InventarioServiceImpl
├── repository/
│   ├── DepositoRepository
│   ├── TipoGarrafaRepository
│   ├── EstadoGarrafaRepository
│   ├── StockGarrafaRepository
│   └── MovimientoGarrafaRepository
├── model/
│   ├── Deposito
│   ├── TipoGarrafaStock
│   ├── EstadoGarrafa
│   ├── StockGarrafa
│   └── MovimientoGarrafa
├── dto/
│   ├── request/
│   │   ├── DepositoRequest
│   │   ├── TransferenciaRequest
│   │   ├── VentaStockRequest
│   │   ├── DevolucionRequest
│   │   ├── AjusteInventarioRequest
│   │   ├── CargaCamionRequest
│   │   └── DescargaCamionRequest
│   └── response/
│       ├── DepositoResponse
│       ├── StockGarrafaResponse
│       ├── StockDepositoResponse
│       ├── StockTotalResponse
│       ├── MovimientoResponse
│       └── MovimientoPageResponse
├── mapper/
│   ├── DepositoMapper
│   ├── StockGarrafaMapper
│   └── MovimientoMapper
└── enums/
    ├── TipoDeposito
    ├── EstadoGarrafaEnum
    └── TipoMovimiento
```

### Responsabilidades

| Clase | Responsabilidad |
|---|---|
| `DepositoController` | Exponer CRUD de depósitos vía REST |
| `StockController` | Exponer consultas de stock por depósito, camión, total |
| `MovimientoController` | Exponer operaciones que generan movimientos |
| `DepositoService` | Crear, editar, activar/desactivar depósitos |
| `StockService` | Consultar stock actual por depósito/tipo/estado |
| `MovimientoStockService` | Registrar cualquier movimiento (venta, devolución, rotura, etc.) |
| `TransferenciaService` | Orquestar transferencias entre depósitos validando disponibilidad |
| `InventarioService` | Ajustes manuales de inventario con trazabilidad |
| `StockGarrafaRepository` | Acceso a tabla `stock_garrafa` con bloqueos cuando se requiera |
| `MovimientoGarrafaRepository` | Persistir y consultar historial de movimientos |
| `Deposito` | Entidad que representa cualquier ubicación (depósito, camión, sucursal, etc.) |
| `StockGarrafa` | Stock de un tipo+estado de garrafa en un depósito específico |
| `MovimientoGarrafa` | Registro inmutable de cada operación |

---

## 2. Modelo de datos

### Tabla: `depositos`
| Columna | Tipo | Descripción |
|---|---|---|
| id | BIGSERIAL PK | |
| nombre | VARCHAR(100) NOT NULL | |
| tipo | VARCHAR(30) NOT NULL | DEPOSITO_CENTRAL, CAMION, SUCURSAL, PLANTA, TALLER |
| descripcion | VARCHAR(255) | |
| activo | BOOLEAN NOT NULL DEFAULT true | |
| vehiculo_patente | VARCHAR(20) | Solo para tipo CAMION |
| repartidor_id | BIGINT FK → usuarios | Solo para tipo CAMION |
| created_at | TIMESTAMP | |
| updated_at | TIMESTAMP | |
| created_by | VARCHAR(100) | |
| updated_by | VARCHAR(100) | |

### Tabla: `tipos_garrafa_stock`
| Columna | Tipo | Descripción |
|---|---|---|
| id | BIGSERIAL PK | |
| codigo | VARCHAR(20) UNIQUE NOT NULL | Ej: "10KG", "15KG", "45KG" |
| descripcion | VARCHAR(100) NOT NULL | |
| capacidad_kg | INTEGER NOT NULL | |
| activo | BOOLEAN NOT NULL DEFAULT true | |

**Nota**: Esta tabla coexiste con el enum `TipoGarrafa` existente. Se puede mapear por código.

### Tabla: `estados_garrafa`
| Columna | Tipo | Descripción |
|---|---|---|
| id | BIGSERIAL PK | |
| codigo | VARCHAR(30) UNIQUE NOT NULL | LLENA, VACIA, RESERVADA, REPARACION, FUERA_SERVICIO |
| descripcion | VARCHAR(100) | |
| activo | BOOLEAN NOT NULL DEFAULT true | |

### Tabla: `stock_garrafa`
| Columna | Tipo | Descripción |
|---|---|---|
| id | BIGSERIAL PK | |
| deposito_id | BIGINT FK NOT NULL | |
| tipo_garrafa_id | BIGINT FK NOT NULL | |
| estado_garrafa_id | BIGINT FK NOT NULL | |
| cantidad | INTEGER NOT NULL DEFAULT 0 | Nunca negativo |
| version | BIGINT NOT NULL DEFAULT 0 | Optimistic Locking |
| updated_at | TIMESTAMP | |
| UNIQUE | (deposito_id, tipo_garrafa_id, estado_garrafa_id) | |

### Tabla: `movimientos_garrafa`
| Columna | Tipo | Descripción |
|---|---|---|
| id | BIGSERIAL PK | |
| tipo_movimiento | VARCHAR(30) NOT NULL | VENTA, DEVOLUCION, TRANSFERENCIA, AJUSTE, CARGA, DESCARGA, ROTURA, REPARACION |
| deposito_origen_id | BIGINT FK | Null si es ingreso inicial |
| deposito_destino_id | BIGINT FK | Null si es baja definitiva |
| tipo_garrafa_id | BIGINT FK NOT NULL | |
| estado_origen_id | BIGINT FK | Estado antes |
| estado_destino_id | BIGINT FK NOT NULL | Estado después |
| cantidad | INTEGER NOT NULL | |
| pedido_id | BIGINT FK | Opcional (si viene de un pedido) |
| usuario_id | BIGINT FK NOT NULL | Quién realizó la operación |
| fecha | TIMESTAMP NOT NULL | |
| observaciones | TEXT | |

---

## 3. Enums

### TipoDeposito
```
DEPOSITO_CENTRAL, CAMION, SUCURSAL, PLANTA, TALLER
```

### TipoMovimiento
```
VENTA          → origen: camión,  destino: null (stock sale del sistema)
DEVOLUCION     → origen: null,    destino: camión o depósito
TRANSFERENCIA  → origen: depX,    destino: depY
CARGA_CAMION   → origen: depósito central, destino: camión
DESCARGA_CAMION→ origen: camión,  destino: depósito central
AJUSTE_ENTRADA → origen: null,    destino: depósito (corrección manual)
AJUSTE_SALIDA  → origen: depósito, destino: null (corrección manual)
ROTURA         → origen: depósito, destino: null (baja definitiva)
REPARACION     → cambia estado de REPARACION → LLENA/FUERA_SERVICIO
```

### EstadoGarrafaEnum
```
LLENA, VACIA, RESERVADA, REPARACION, FUERA_SERVICIO
```

---

## 4. Casos de uso

### Depósitos
- UC-01: Crear depósito
- UC-02: Editar depósito
- UC-03: Activar/Desactivar depósito
- UC-04: Listar depósitos
- UC-05: Obtener depósito por ID

### Tipos de garrafa y estados
- UC-06: Crear tipo de garrafa
- UC-07: Editar tipo de garrafa
- UC-08: Listar tipos de garrafa
- UC-09: Crear estado de garrafa
- UC-10: Listar estados de garrafa

### Operaciones de stock
- UC-11: Cargar camión (transferir del depósito central al camión)
- UC-12: Descargar camión (transferir del camión al depósito central)
- UC-13: Transferir stock entre depósitos
- UC-14: Registrar venta (el repartidor entrega llena, recibe vacía)
- UC-15: Registrar devolución (cliente devuelve garrafa)
- UC-16: Registrar rotura (baja definitiva)
- UC-17: Registrar entrada a reparación (LLENA/VACIA → REPARACION)
- UC-18: Registrar salida de reparación (REPARACION → LLENA o FUERA_SERVICIO)
- UC-19: Ajuste de inventario (entrada manual)
- UC-20: Ajuste de inventario (salida manual)

### Consultas
- UC-21: Consultar stock de un depósito
- UC-22: Consultar stock de un camión
- UC-23: Consultar stock total (suma de todos los depósitos)
- UC-24: Consultar historial de movimientos (con filtros)
- UC-25: Consultar movimientos de un depósito
- UC-26: Consultar movimientos de un pedido

---

## 5. Diseño de Servicios

### DepositoService
Responsabilidad: CRUD de depósitos. Valida unicidad de nombre, tipo CAMION requiere patente.

### StockService
Responsabilidad: Consultas de stock. No modifica datos.
- `getStockByDeposito(Long depositoId)` → lista de StockGarrafaResponse
- `getStockByDepositoYTipo(Long depositoId, Long tipoId)` → StockGarrafaResponse
- `getStockTotal()` → StockTotalResponse (agrupado por tipo+estado)
- `getStockCamion(Long depositoId)` → StockDepositoResponse

### MovimientoStockService
Responsabilidad: Registrar cualquier movimiento. Es el único punto de escritura sobre `stock_garrafa`. Toda escritura genera un registro en `movimientos_garrafa`. Se anota `@Transactional`.

Métodos internos:
- `decrementarStock(Deposito, TipoGarrafa, EstadoGarrafa, int cantidad)`
- `incrementarStock(Deposito, TipoGarrafa, EstadoGarrafa, int cantidad)`
- `registrarMovimiento(MovimientoGarrafa)`

### TransferenciaService
Responsabilidad: Orquestar transferencias. Llama a `MovimientoStockService` para decrementar origen e incrementar destino en una sola transacción.
- `transferir(TransferenciaRequest, Usuario)` → MovimientoResponse
- `cargarCamion(CargaCamionRequest, Usuario)` → MovimientoResponse
- `descargarCamion(DescargaCamionRequest, Usuario)` → MovimientoResponse

### InventarioService
Responsabilidad: Operaciones de negocio complejas: venta, devolución, rotura, reparación, ajuste.
- `registrarVenta(VentaStockRequest, Usuario)` → MovimientoResponse
- `registrarDevolucion(DevolucionRequest, Usuario)` → MovimientoResponse
- `registrarRotura(Long depositoId, Long tipoId, int cantidad, Usuario)` → MovimientoResponse
- `iniciarReparacion(Long depositoId, Long tipoId, int cantidad, Usuario)` → MovimientoResponse
- `finalizarReparacion(Long depositoId, Long tipoId, int cantidad, EstadoDestino, Usuario)` → MovimientoResponse
- `ajusteEntrada(AjusteInventarioRequest, Usuario)` → MovimientoResponse
- `ajusteSalida(AjusteInventarioRequest, Usuario)` → MovimientoResponse
