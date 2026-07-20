# Indicaciones para el frontend (entorno web)

> Asumimos: SPA o PWA con JWT en `Authorization: Bearer ...`. Si tenes service worker + IndexedDB para offline, esto encaja bien; si no, podes ignorar la parte offline.

---

## A. Resumen de endpoints

| Accion | Endpoint | Auth |
|---|---|---|
| Crear cliente | `POST /api/clientes` (JSON) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| **Subir primera foto de cliente** | `POST /api/clientes/{clienteId}/foto` (multipart) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| **Reemplazar foto de cliente** | `PUT /api/clientes/{clienteId}/foto` (multipart) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| Obtener cliente | `GET /api/clientes/{clienteId}` | cualquier autenticado |
| Crear pedido | `POST /api/pedidos` (JSON) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| Obtener pedido | `GET /api/pedidos/{id}` o `GET /api/pedidos/uuid/{uuid}` | PREVENTISTA, ADMIN, SUPER_ADMIN |
| **Subir imagen offline** | `POST /api/sincronizacion/clientes/imagenes?clienteId=X` (multipart) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| Sincronizar pedido offline | `POST /api/sincronizacion` (JSON) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| **Listar todas las rutas (panel admin)** | `GET /api/rutas?fechaDesde=YYYY-MM-DD&fechaHasta=YYYY-MM-DD&repartidorId=N&limit=N` | ADMIN, SUPER_ADMIN |
| ~~Subir foto al pedido~~ | `POST /api/pedidos/{id}/foto` | **DEPRECADO**, migrar a endpoints de cliente |
| **Tracking GPS en tiempo real (STOMP)** | `WS /ws?token=<jwt>` + `/app/rutas/{rutaId}/posicion` | REPARTIDOR (publica), ADMIN/SUPER_ADMIN (se suscribe a `/topic/rutas/{rutaId}/posiciones`) |

### Tracking en tiempo real (STOMP/WebSocket)

Pensado para que el panel admin dibuje la posicion del repartidor en vivo sobre un mapa
(LocationIQ en el front) y para que la app del repartidor publique su GPS cada N segundos.

**Conexion (el front abre una sola sesion STOMP por usuario):**

```ts
// Angular / @stomp/ng2-stompjs
const stompConfig: StompConfig = {
  url: () => new SockJS(`/ws?token=${encodeURIComponent(jwt)}`),
  // headers_connect: { Authorization: `Bearer ${jwt}` }, // alternativa al query
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
};
```

**Endpoint nativo (sin SockJS):** `ws://host:8080/ws-native?token=<jwt>` — útil para clientes
mobile que no quieren el fallback SockJS.

**Desde el panel admin (suscribirse a las posiciones de una ruta):**

```ts
stompService.subscribe(`/topic/rutas/${rutaId}/posiciones`).subscribe(msg => {
  const pos: PosicionBroadcastDto = JSON.parse(msg.body);
  mapService.actualizarMarker(pos.repartidorId, [pos.latitud, pos.longitud], pos.headingGrados);
});

stompService.subscribe(`/topic/rutas/${rutaId}/eventos`).subscribe(msg => {
  const evt: EventoRutaWsDto = JSON.parse(msg.body);
  if (evt.tipo === 'CAMBIO_ESTADO_RUTA') {
    toast.info(`Ruta ${evt.rutaId}: ${evt.estadoAnterior} -> ${evt.estadoNuevo}`);
  }
});

// errores per-usuario (validacion / autorizacion)
stompService.subscribe('/user/queue/errors').subscribe(msg => {
  const err: ErrorWsDto = JSON.parse(msg.body);
  toast.error(`${err.codigo}: ${err.mensaje}`);
});
```

**Desde la app del repartidor (publicar la posicion GPS):**

```ts
// Disparar cada N segundos (ej: 5s) con la API de geolocation del dispositivo
watchId = navigator.geolocation.watchPosition(
  ({ coords, timestamp }) => {
    stompService.publish({
      destination: `/app/rutas/${rutaId}/posicion`,
      body: JSON.stringify({
        latitud: coords.latitude,
        longitud: coords.longitude,
        headingGrados: coords.heading,
        velocidadMps: coords.speed,
        precisionM: coords.accuracy,
        timestampCliente: new Date(timestamp).toISOString(),
        origen: 'GPS',
      }),
    });
  },
  err => console.warn('Geolocation error', err),
  { enableHighAccuracy: true, maximumAge: 1000, timeout: 5000 }
);
```

**Contratos (DTOs JSON):**

```ts
// Lo que envia el repartidor al server
interface PosicionRepartidorDto {
  rutaId?: number;
  latitud: number;             // -90..90
  longitud: number;            // -180..180
  headingGrados?: number;      // 0..360
  velocidadMps?: number;
  precisionM?: number;
  timestampCliente?: string;   // ISO-8601
  origen?: 'GPS' | 'NETWORK' | 'MANUAL';
}

// Lo que el server retransmite al topico (incluye datos del repartidor y sello del server)
interface PosicionBroadcastDto {
  rutaId: number;
  repartidorId: number;
  repartidorNombre: string;
  estadoRuta: 'PLANIFICADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA';
  latitud: number;
  longitud: number;
  headingGrados?: number;
  velocidadMps?: number;
  precisionM?: number;
  timestampCliente?: string;
  serverTimestamp: string;
  origen?: 'GPS' | 'NETWORK' | 'MANUAL';
}

// Eventos automaticos disparados por el backend ante cambios de estado o paradas
interface EventoRutaWsDto {
  tipo: 'CAMBIO_ESTADO_RUTA' | 'CAMBIO_ESTADO_PARADA' | 'RUTA_CANCELADA';
  rutaId: number;
  repartidorId?: number;
  estadoAnterior?: 'PLANIFICADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA';
  estadoNuevo?: 'PLANIFICADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA';
  rutaPedidoId?: number;
  mensaje?: string;
  timestamp: string;
}

// Errores per-usuario (autorizacion, validacion)
interface ErrorWsDto {
  codigo:
    | 'NO_AUTH'
    | 'PAYLOAD_VACIO'
    | 'RUTA_ID_REQUERIDO'
    | 'COORDENADAS_REQUERIDAS'
    | 'LATITUD_INVALIDA'
    | 'LONGITUD_INVALIDA'
    | 'TIMESTAMP_MUY_VIEJO'
    | 'TIMESTAMP_MUY_FUTURO'
    | 'RUTA_NO_ENCONTRADA'
    | 'RUTA_NO_PROPIA'
    | 'RUTA_NO_TRANSMITE'
    | 'FORBIDDEN_NOT_REPARTIDOR'
    | 'BAD_REQUEST';
  mensaje: string;
  timestamp: string;
}
```

**Reglas importantes:**

* El token JWT debe ser el mismo que el del endpoint REST (mismo secreto, misma firma).
* En `producción` es preferible mandar el token en el header `Authorization: Bearer ...`
  del handshake para que no quede en los access-logs del proxy.
* El server **no** persiste posiciones: solo retransmite. Para guardar un historico
  delgado hay que modificar `TrackingServiceImpl` y agregar la columna correspondiente
  en una nueva migracion Flyway (`V20__...`).

### Panel admin — listado de rutas de reparto

```http
GET /api/rutas?fechaDesde=2026-07-01&fechaHasta=2026-07-17&repartidorId=5&limit=50
Authorization: Bearer <jwt-admin>
```

**Query params (todos opcionales):**

- `fechaDesde` (LocalDate, default `hoy − 30 dias`)
- `fechaHasta` (LocalDate, default `hoy`)
- `repartidorId` (Long, default `null`)
- `limit` (Integer 1‑1000, default `100`)

Devuelve `200 OK` con `List<RutaResponse>` (mismo DTO que `/api/rutas/planificar` y
`/api/rutas/mis-rutas/{id}`), ordenado por `updatedAt DESC, id DESC`. Si no hay
resultados, devuelve `[]` (no es 404).

**Para traer todo el historial sin paginar manualmente:**

```
GET /api/rutas?fechaDesde=2020-01-01&fechaHasta=2026-12-31&limit=1000
```

**Para mostrar en el panel:**

```ts
// Pseudo-codigo Angular
listarRutas(filtros: { fechaDesde?: string; fechaHasta?: string; repartidorId?: number }) {
  const params = new URLSearchParams();
  if (filtros.fechaDesde)    params.set('fechaDesde', filtros.fechaDesde);
  if (filtros.fechaHasta)    params.set('fechaHasta', filtros.fechaHasta);
  if (filtros.repartidorId)  params.set('repartidorId', String(filtros.repartidorId));
  return this.http.get<RutaResponse[]>(`/api/rutas?${params}`);
}
```

---

## B. Flujo online (preventista con conexion)

### Crear un cliente nuevo con foto

```
1. POST /api/clientes
   body: { nombre, telefono, direccion }
   -> { id: 7, urlFotoEvidencia: null, ... }

2. POST /api/clientes/7/foto
   multipart: archivo=<jpg>, descripcion="Fachada principal"
   -> { clienteId: 7, objectPath: "cliente-7/uuid.jpg", urlFotoEvidencia: "https://...signed...", ... }
   ATENCION: si el cliente ya tiene foto, devuelve 409. Usar PUT para reemplazar.
```

### Crear un pedido (cliente ya existe, con o sin foto)

```
1. POST /api/pedidos
   body: { uuidOffline, clienteId, direccionEntrega, detalles: [...] }
   -> { id: 42, clienteId: 7, urlFotoEvidencia: "https://...signed...", detalles: [...] }
   (El urlFotoEvidencia viene del cliente. Si el cliente no tiene foto, viene null.)
```

### Reemplazar la foto de un cliente

```
PUT /api/clientes/7/foto
multipart: archivo=<nuevo-jpg>, descripcion="Fachada reformada"
-> { clienteId: 7, objectPath: "cliente-7/nuevo-uuid.jpg", urlFotoEvidencia: "https://...signed..." }
ATENCION: si el cliente NO tiene foto previa, devuelve 409. Usar POST.
```

---

## C. Flujo offline (preventista sin conexion)

> Requiere IndexedDB. Te damos la logica, no el codigo exacto (depende de tu stack).

### Paso 1 -- IndexedDB: stores necesarias

```js
// Pseudo-schema
clientes:    { id (local), nombre, telefono, direccion, serverId, fotoPath, fotoBlob }
pedidos:     { uuidOffline, clienteLocalId, clienteServerId, direccionEntrega, detalles, fotoParaSubir? }
syncQueue:   { id, tipo: 'cliente'|'pedido'|'imagen', payload, intentos, ultimoError }
```

### Paso 2 -- Al crear un pedido offline (cliente nuevo o existente)

```js
if (!navigator.onLine) {
  // 1. Guardar cliente en IndexedDB si es nuevo (sin serverId todavia)
  // 2. Guardar pedido con uuidOffline
  // 3. Si el cliente no tiene foto Y el preventista tomo foto:
  //    - Guardar el Blob en el pedido o en un store aparte de "imagenesPendientes"
  //    - Marcar pedido.fotoParaSubir = true
  // 4. Encolar en syncQueue
}
```

### Paso 3 -- Al detectar conexion (`window.addEventListener('online', ...)` o `navigator.onLine`)

```js
async function sincronizar() {
  // 1. Primero subir clientes (si hay)
  for (const c of clientes donde serverId == null) {
    POST /api/clientes
    if (409) { /* ya existe, fetchear por telefono o similar */ }
  }

  // 2. Subir imagenes pendientes ANTES que los pedidos
  for (const p of pedidos donde fotoParaSubir == true) {
    POST /api/sincronizacion/clientes/imagenes?clienteId={serverId}
         multipart: archivo=<blob>, uuidOffline=...
    if (409) { /* ya hay foto o ya hay imagen pendiente, marcar como hecho */ }
    p.fotoParaSubir = false  // marcar para no reintentar
  }

  // 3. Subir pedidos en lote
  POST /api/sincronizacion
  body: { pedidos: [...] }
  // El backend asocia automaticamente las imagenes pendientes
}
```

### Paso 4 -- Errores comunes a manejar

| Status | Codigo | Significado | Accion |
|---|---|---|---|
| 200 | - | OK | Limpiar de syncQueue |
| 400 | `GARRAFA_TIPO_OBLIGATORIO` / `GARRAFA_CAPACIDAD_OBLIGATORIA` / `GARRAFA_CAPACIDAD_INCONSISTENTE` | Garrafa invalida al crear/actualizar | Mostrar mensaje al usuario; no reintentar |
| 400 | `PEDIDO_DUPLICADO` | UUID offline ya existe en el server | Marcar como exitoso y descargar el serverId |
| 401 | `NO_AUTHENTICATED` | Sin token / token invalido | Redirigir a login |
| 404 | `USUARIO_NO_ENCONTRADO` | Token valido pero usuario borrado de BD | Forzar re-login |
| 404 | (generico) | Cliente o pedido no existe en server | Re-fetch y reintentar |
| 409 en `/clientes/imagenes` | - | Cliente ya tiene foto o ya hay imagen pendiente | Marcar como hecho y continuar (no reintentar) |
| 409 en `/clientes/{id}/foto` POST | - | Cliente ya tiene foto | Usar PUT o no subir |
| 409 en `/clientes/{id}/foto` PUT | - | Cliente no tiene foto previa | Usar POST o no subir |
| 5xx | - | Error del servidor | Reintentar con backoff exponencial |

---

## D. Como mostrar la foto del cliente

La URL que viene en `urlFotoEvidencia` es una **signed URL** que expira (default 1 hora). Opciones:

1. **Cachear la respuesta del pedido/cliente** (no la URL puntual). Cada vez que el usuario abre el pedido, fetchear el pedido y usar la URL fresca.
2. **Refetch bajo demanda**: si la imagen falla al cargar, fetchear el pedido de nuevo y reintentar.

```js
// Pseudo-codigo
async function mostrarFotoCliente(clienteId) {
  const cliente = await fetch(`/api/clientes/${clienteId}`).then(r => r.json());
  if (!cliente.urlFotoEvidencia) return null;  // cliente sin foto
  return cliente.urlFotoEvidencia;  // signed URL, usar directo en <img src>
}

<img [src]="fotoUrl" (error)="refrescarFoto()" />
```

---

## E. Headers y errores

- **Auth**: todas las requests llevan `Authorization: Bearer <jwt>`.
- **Content-Type**: `application/json` para los DTOs, `multipart/form-data` para uploads.

### Formato de respuesta (envelope `ApiResponse`)

Todas las respuestas (exitosas o de error) usan el mismo envelope:

```json
{
  "exito": true,
  "mensaje": "Operacion exitosa",
  "data": { /* payload o null */ },
  "timestamp": "2026-07-14T15:30:00Z"
}
```

En errores, el `data` no es null sino un objeto con el codigo estructurado:

```json
{
  "exito": false,
  "mensaje": "Usuario autenticado no encontrado en la base de datos",
  "data": {
    "codigo": "USUARIO_NO_ENCONTRADO",
    "status": 404
  },
  "timestamp": "2026-07-14T15:30:00Z"
}
```

Recomendaciones:

- **Errores 4xx**: leer `body.mensaje` para mostrar al usuario y `body.data.codigo` para
  logica condicional (ej: si `codigo === "NO_AUTHENTICATED"` redirigir a login).
- **Errores 5xx**: loguear `body.data.codigo` (si esta presente) y mostrar "reintentar".
- **Lista completa de codigos**: ver seccion "Codigos de error" del README.

---

## F. Resumen de la migracion desde el flujo viejo

| Antes (deprecated) | Ahora |
|---|---|
| `POST /api/pedidos/{id}/foto` despues de crear el pedido | `POST /api/clientes/{clienteId}/foto` antes o despues del primer pedido |
| La foto se subia una vez por pedido | La foto se sube una vez por cliente |
| La URL era publica | La URL es firmada (expira en 1h) |
| Sin soporte offline | Soporte offline via `/sincronizacion/clientes/imagenes` |
