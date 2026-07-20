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

> **Documentacion completa:** ver [`docs/WEBSOCKETS_FRONTEND.md`](WEBSOCKETS_FRONTEND.md) — incluye
> snippets Angular completos, version mobile (Capacitor/React Native), pruebas manuales con
> `wscat` y casos borde. Esta seccion es solo un resumen rapido.

**Conexion (el front abre una sola sesion STOMP por usuario):**

| Endpoint | Transporte | Uso |
|---|---|---|
| `ws://host/ws` | SockJS (xhr-streaming, long-polling) | SPA web |
| `ws://host/ws-native` | WebSocket nativo | Mobile nativo, tests |

**Auth:** JWT en `?token=...` (mas compatible) o en `Authorization: Bearer ...` en el
handshake HTTP. Mismo JWT que devuelve `POST /api/auth/login`.

**Snippet minimo (Angular + @stomp/stompjs + SockJS):**

```ts
const client = new Client({
  webSocketFactory: () => new SockJS(`/ws?token=${encodeURIComponent(jwt)}`),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});

client.onConnect = () => {
  // Admin: subscribirse a las posiciones
  client.subscribe(`/topic/rutas/${rutaId}/posiciones`, msg => {
    const pos: PosicionBroadcastDto = JSON.parse(msg.body);
    mapService.moverMarker(pos.repartidorId, [pos.latitud, pos.longitud]);
  });

  // Repartidor: publicar cada N segundos
  navigator.geolocation.watchPosition(({ coords, timestamp }) => {
    client.publish({
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
  });
};

client.activate();
```

**Topicos:**

| Direccion | Destino | Payload |
|---|---|---|
| Cliente → Server | `/app/rutas/{rutaId}/posicion` | `PosicionRepartidorDto` |
| Server → Cliente | `/topic/rutas/{rutaId}/posiciones` | `PosicionBroadcastDto` |
| Server → Cliente | `/topic/rutas/{rutaId}/eventos` | `EventoRutaWsDto` |
| Server → Cliente (per-user) | `/user/queue/errors` | `ErrorWsDto` |

**Reglas del server:**

* Latitud `[-90, 90]` y longitud `[-180, 180]` obligatorias.
* `timestampCliente` con drift > 5 min se rechaza (`TIMESTAMP_MUY_VIEJO` / `TIMESTAMP_MUY_FUTURO`).
* La ruta debe estar en `PLANIFICADA` o `EN_CURSO` para aceptar tracking.
* Un REPARTIDOR no puede subscribirse a la ruta de otro (rechazo silencioso del SUBSCRIBE).
* El server **no persiste** posiciones: solo retransmite.

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
