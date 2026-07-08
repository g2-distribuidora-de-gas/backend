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
| ~~Subir foto al pedido~~ | `POST /api/pedidos/{id}/foto` | **DEPRECADO**, migrar a endpoints de cliente |

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

| Status | Significado | Accion |
|---|---|---|
| 200 | OK | Limpiar de syncQueue |
| 409 en `/clientes/imagenes` | Cliente ya tiene foto o ya hay imagen pendiente | Marcar como hecho y continuar (no reintentar) |
| 409 en `/clientes/{id}/foto` POST | Cliente ya tiene foto | Usar PUT o no subir |
| 409 en `/clientes/{id}/foto` PUT | Cliente no tiene foto previa | Usar POST o no subir |
| 404 | Cliente o pedido no existe en server | Re-fetch y reintentar |
| 5xx | Error del servidor | Reintentar con backoff exponencial |

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
- **Errores 4xx**: leer `body.message` (o `body.error`) para mostrar al usuario.
- **Errores 5xx**: loguear y mostrar "reintentar".

---

## F. Resumen de la migracion desde el flujo viejo

| Antes (deprecated) | Ahora |
|---|---|
| `POST /api/pedidos/{id}/foto` despues de crear el pedido | `POST /api/clientes/{clienteId}/foto` antes o despues del primer pedido |
| La foto se subia una vez por pedido | La foto se sube una vez por cliente |
| La URL era publica | La URL es firmada (expira en 1h) |
| Sin soporte offline | Soporte offline via `/sincronizacion/clientes/imagenes` |
