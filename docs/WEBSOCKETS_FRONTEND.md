# Tracking GPS en tiempo real — Guía de integración para el frontend

> Cómo conectarse al WebSocket/STOMP del backend para mostrar la posición del repartidor
> en vivo sobre un mapa (LocationIQ) y cómo la app del repartidor publica su GPS.
>
> **Stack asumido:** SPA / PWA / app híbrida con `sockjs-client` + `@stomp/stompjs`
> (Angular, React, Vue, Ionic/Capacitor, etc.). Si el cliente puede hacer WebSocket directo
> también funciona, sin SockJS.

---

## Índice

1. [Visión general](#1-visión-general)
2. [Conexión al WebSocket](#2-conexión-al-websocket)
3. [Topicos STOMP](#3-topicos-stomp)
4. [Modelo de datos (DTOs)](#4-modelo-de-datos-dtos)
5. [Panel admin — visualizar el recorrido](#5-panel-admin--visualizar-el-recorrido)
6. [App repartidor — publicar la posición](#6-app-repartidor--publicar-la-posición)
7. [Errores y códigos](#7-errores-y-códigos)
8. [Casos borde y reglas de validación](#8-casos-borde-y-reglas-de-validación)
9. [Snippet completo (Angular)](#9-snippet-completo-angular)
10. [Snippet completo (Ionic / Capacitor / React Native)](#10-snippet-completo-ionic--capacitor--react-native)
11. [Pruebas manuales](#11-pruebas-manuales)

---

## 1. Visión general

```
┌────────────────┐  SEND /app/rutas/{id}/posicion   ┌─────────────────┐
│ App repartidor │ ─────────────────────────────────► │  Backend Spring │
│ (móvil)        │                                    │  + STOMP broker │
│                │                                    │                 │
│                │ ◄── broadcast /topic/... ────────  │                 │
└────────────────┘                                    └─────────────────┘
                                                              │
                                                              │ broadcast
                                                              ▼
                                                    ┌─────────────────┐
                                                    │ Panel admin      │
                                                    │ (LocationIQ map) │
                                                    └─────────────────┘
```

**Reglas de oro:**

- El **repartidor PUBLICA** su posición (solo la de sus rutas activas).
- El **admin / super_admin SE SUSCRIBE** a los tópicos de las rutas que quiere ver.
- Un repartidor NO puede ver la ruta de otro (rechazado en el `SUBSCRIBE`).
- El server **no persiste** las posiciones: solo retransmite. Para histórico, hablar con backend.

---

## 2. Conexión al WebSocket

### Endpoints disponibles

| URL | Transporte | Cuándo usarlo |
|---|---|---|
| `ws(s)://host/ws` | SockJS (xhr-streaming, long-polling como fallback) | SPA web, entornos con proxies restrictivos |
| `ws(s)://host/ws-native` | WebSocket nativo (sin SockJS) | Mobile nativo (Capacitor, RN), tests |

### Autenticación

El backend espera el JWT en **uno** de estos dos lugares al hacer el handshake HTTP→WS:

| Opción | Header / Query | Notas |
|---|---|---|
| **Query param** (recomendado para SockJS) | `?token=<jwt>` | Funciona siempre. En prod, asegurar que el proxy no loggee la URL completa. |
| **Header** (recomendado para nativo) | `Authorization: Bearer <jwt>` | Más limpio, no aparece en access logs. |

El token es el mismo JWT que devuelve `POST /api/auth/login` y que se usa en los endpoints REST.

### Handshake OK / Error

| HTTP al handshake | Significado | Acción del cliente |
|---|---|---|
| `101 Switching Protocols` | OK, conexión STOMP abierta | enviar `CONNECT` |
| `401 Unauthorized` | Token ausente / inválido / expirado | refrescar token y reintentar |
| `403 Forbidden` | (no debería pasar — el interceptor es el que valida) | log + reintentar |

---

## 3. Topicos STOMP

### Cliente → Servidor (enviar mensajes)

El cliente **PUBLICA** (envía) en estos destinos. Todos llevan prefijo `/app/`
(lo quita STOMP automáticamente antes de llegar al controller):

| Destino | Payload | Quién envía | Descripción |
|---|---|---|---|
| `/app/rutas/{rutaId}/posicion` | `PosicionRepartidorDto` | REPARTIDOR | Reporta una coordenada GPS |

### Servidor → Cliente (recibir mensajes)

El server **RETRANSMITE** en estos destinos. Todos llevan prefijo `/topic/`
excepto `/user/...` que es per-usuario:

| Destino | Payload | Quién se suscribe | Descripción |
|---|---|---|---|
| `/topic/rutas/{rutaId}/posiciones` | `PosicionBroadcastDto` | ADMIN, SUPER_ADMIN, o el REPARTIDOR dueño | Posición actual del repartidor de esa ruta |
| `/topic/rutas/{rutaId}/eventos` | `EventoRutaWsDto` | cualquiera autenticado | Cambios de estado de la ruta o de paradas |
| `/user/queue/errors` | `ErrorWsDto` | el propio usuario | Errores de validación / autorización per-usuario |

> **Permisos en SUBSCRIBE:** el servidor valida antes de aceptar la suscripción.
> Si un REPARTIDOR intenta subscribirse a la ruta de otro, recibe un `ERROR` frame STOMP
> y la suscripción se descarta. Esto es transparente: el `client.subscribe(...)` simplemente
> nunca recibe mensajes para ese destino.

---

## 4. Modelo de datos (DTOs)

Todos los timestamps son ISO-8601 en UTC (`2026-07-20T15:30:00Z`).
Los decimales usan `BigDecimal` en el server pero en JSON van como `number`.

### `PosicionRepartidorDto` — cliente → server

```ts
interface PosicionRepartidorDto {
  rutaId?: number;             // opcional; el server usa el del path de todas formas
  latitud: number;             // requerido, -90..90
  longitud: number;            // requerido, -180..180
  headingGrados?: number;      // opcional, 0..360 (rumbo del dispositivo)
  velocidadMps?: number;       // opcional, m/s
  precisionM?: number;         // opcional, metros (accuracy del GPS)
  timestampCliente?: string;   // opcional, ISO-8601. Si se omite, el server usa Instant.now()
  origen?: 'GPS' | 'NETWORK' | 'MANUAL'; // opcional, default GPS
}
```

### `PosicionBroadcastDto` — server → topic

```ts
interface PosicionBroadcastDto {
  rutaId: number;
  repartidorId: number;
  repartidorNombre: string;            // "Juan Perez"
  estadoRuta: 'PLANIFICADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA';
  latitud: number;
  longitud: number;
  headingGrados?: number;
  velocidadMps?: number;
  precisionM?: number;
  timestampCliente?: string;
  serverTimestamp: string;             // UTC, siempre presente
  origen?: 'GPS' | 'NETWORK' | 'MANUAL';
}
```

### `EventoRutaWsDto` — server → topic

```ts
interface EventoRutaWsDto {
  tipo: 'CAMBIO_ESTADO_RUTA' | 'CAMBIO_ESTADO_PARADA' | 'RUTA_CANCELADA';
  rutaId: number;
  repartidorId?: number;
  estadoAnterior?: 'PLANIFICADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA';
  estadoNuevo?: 'PLANIFICADA' | 'EN_CURSO' | 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA';
  rutaPedidoId?: number;               // presente si tipo === 'CAMBIO_ESTADO_PARADA'
  mensaje?: string;
  timestamp: string;
}
```

### `ErrorWsDto` — server → user

```ts
interface ErrorWsDto {
  codigo:
    | 'NO_AUTH'                 // sesión sin identidad (raro, bug del cliente)
    | 'PAYLOAD_VACIO'           // payload null
    | 'RUTA_ID_REQUERIDO'       // falta rutaId
    | 'COORDENADAS_REQUERIDAS'  // falta lat o lng
    | 'LATITUD_INVALIDA'        // fuera de [-90, 90]
    | 'LONGITUD_INVALIDA'       // fuera de [-180, 180]
    | 'TIMESTAMP_MUY_VIEJO'     // > 5 min en el pasado (configurable)
    | 'TIMESTAMP_MUY_FUTURO'    // > 1 min en el futuro (configurable)
    | 'RUTA_NO_ENCONTRADA'      // rutaId no existe
    | 'RUTA_NO_PROPIA'          // ruta pertenece a otro repartidor
    | 'RUTA_NO_TRANSMITE'       // ruta está COMPLETADA/CANCELADA/REPROGRAMADA
    | 'FORBIDDEN_NOT_REPARTIDOR'// un no-repartidor intentó publicar
    | 'BAD_REQUEST'             // error genérico de validación
    | 'USUARIO_NO_ENCONTRADO';  // JWT válido pero usuario borrado de BD
  mensaje: string;             // legible para humanos (castellano)
  timestamp: string;
}
```

---

## 5. Panel admin — visualizar el recorrido

### Suscribirse a las posiciones de una ruta

```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  brokerURL: undefined, // usamos SockJS
  webSocketFactory: () => new SockJS(`/ws?token=${jwt}`),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});

client.onConnect = () => {
  // Posiciones en vivo
  client.subscribe(`/topic/rutas/${rutaId}/posiciones`, msg => {
    const pos: PosicionBroadcastDto = JSON.parse(msg.body);
    moverMarkerEnMapa(pos);
  });

  // Eventos de cambio de estado
  client.subscribe(`/topic/rutas/${rutaId}/eventos`, msg => {
    const evt: EventoRutaWsDto = JSON.parse(msg.body);
    manejarEventoRuta(evt);
  });

  // Cola personal de errores
  client.subscribe('/user/queue/errors', msg => {
    const err: ErrorWsDto = JSON.parse(msg.body);
    console.warn(`WS error [${err.codigo}]: ${err.mensaje}`);
  });
};

client.activate();
```

### Pintar el marker con LocationIQ

```ts
function moverMarkerEnMapa(pos: PosicionBroadcastDto) {
  const coords: [number, number] = [pos.longitud, pos.latitud]; // GeoJSON: [lng, lat]

  if (!marker) {
    marker = L.marker(coords, { icon: iconoRepartidor }).addTo(map);
  } else {
    marker.setLatLng(coords);
    if (pos.headingGrados !== undefined) marker.setRotationAngle(pos.headingGrados);
  }

  // Opcional: línea con el recorrido
  if (posPolyline) posPolyline.addLatLng(coords);

  // Edad de la muestra (alerta si > 30s sin updates)
  const edadMs = Date.now() - new Date(pos.serverTimestamp).getTime();
  if (edadMs > 30_000) console.warn(`Posición obsoleta (${edadMs / 1000}s)`);
}
```

---

## 6. App repartidor — publicar la posición

### Loop de geolocalización + WebSocket

```ts
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS(`/ws?token=${jwt}`),
  reconnectDelay: 5000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
});

let watchId: number | null = null;
let ultimoEnvio = 0;

client.onConnect = () => {
  // Solo si la sesion STOMP esta abierta empezamos a publicar
  watchId = navigator.geolocation.watchPosition(
    ({ coords, timestamp }) => {
      const ahora = Date.now();

      // Throttle: no enviar mas de 1 msg cada WS_POS_MIN_INTERVALO_S (default 1s)
      if (ahora - ultimoEnvio < 1000) return;
      ultimoEnvio = ahora;

      client.publish({
        destination: `/app/rutas/${rutaId}/posicion`,
        body: JSON.stringify({
          latitud: coords.latitude,
          longitud: coords.longitude,
          headingGrados: coords.heading ?? null,
          velocidadMps: coords.speed ?? null,
          precisionM: coords.accuracy ?? null,
          timestampCliente: new Date(timestamp).toISOString(),
          origen: 'GPS',
        }),
      });
    },
    err => console.warn('Geolocation error', err),
    { enableHighAccuracy: true, maximumAge: 1000, timeout: 5000 },
  );
};

client.onWebSocketClose = () => {
  if (watchId !== null) {
    navigator.geolocation.clearWatch(watchId);
    watchId = null;
  }
};

client.activate();

// Al cerrar la pantalla de reparto:
function detenerTracking() {
  if (watchId !== null) navigator.geolocation.clearWatch(watchId);
  client.deactivate();
}
```

### Buenas prácticas

- **No publicar si la ruta está cerrada.** Suscribite a `/topic/rutas/{id}/eventos` y si llega
  `estadoNuevo === 'COMPLETADA' | 'CANCELADA' | 'REPROGRAMADA'`, dejá de mandar.
- **Throttle del lado cliente.** El server acepta 1 msg/seg por defecto. Más de eso es ruido.
- **Batería.** Usar `enableHighAccuracy: true` solo con el camión en marcha; en background bajar
  la frecuencia.
- **Permisos.** Pedir permiso de geolocalización apenas se abre la pantalla de reparto.
  Si el usuario rechaza, mostrar CTA para habilitarlo desde settings del SO.

---

## 7. Errores y códigos

### Conexión cerrada por el server

| Causa | Cuándo | Acción del cliente |
|---|---|---|
| Token expirado durante la sesión | el JWT venció mientras el repartidor trabaja | refrescar token, cerrar y reabrir la sesión STOMP |
| Red caída / proxy timeout | heartbeat falla | `client.deactivate()` + reconectar (`reconnectDelay`) |
| Server reiniciado | deploy | mostrar toast "Reconectando..."; el cliente se reintenta solo |

### Errores de validación (llegan a `/user/queue/errors`)

| Código | Significado | Acción recomendada |
|---|---|---|
| `LATITUD_INVALIDA` / `LONGITUD_INVALIDA` | Coords fuera de rango | Bug del cliente. Loggear y descartar. |
| `TIMESTAMP_MUY_VIEJO` | Reloj del dispositivo desincronizado | Re-sincronizar NTP / sugerir calibrar |
| `RUTA_NO_TRANSMITE` | La ruta ya está COMPLETADA/CANCELADA/REPROGRAMADA | Detener el loop de GPS y avisar al repartidor |
| `RUTA_NO_PROPIA` | Estás publicando en la ruta de otro | Bug grave. Detener tracking y forzar re-selección de ruta |
| `FORBIDDEN_NOT_REPARTIDOR` | El usuario conectado no es repartidor | Bug grave. Logout y re-login |
| `USUARIO_NO_ENCONTRADO` | El JWT es válido pero el usuario se borró de BD | Forzar logout + redirigir a login |

---

## 8. Casos borde y reglas de validación

**Lo que valida el server (no tenés que validar en el cliente):**

- Latitud ∈ `[-90, 90]`, longitud ∈ `[-180, 180]`.
- `timestampCliente` no más viejo que **5 minutos** (`WS_POS_MAX_RETRASO_S`, configurable).
- `timestampCliente` no más nuevo que **1 minuto** en el futuro (anti-replay).
- La ruta existe, pertenece al repartidor y está en `PLANIFICADA` o `EN_CURSO`.
- El `rutaId` del payload puede omitirse: el server usa el del path del STOMP message.

**Lo que NO valida el server:**

- Que la velocidad sea realista (un móvil parado reporta 0 m/s, está bien).
- Que `headingGrados` esté en `[0, 360)` (lo guarda como llega).
- Que dos envíos consecutivos no sean duplicados (no hay dedupe).

**No persiste nada.** Si necesitás histórico, hablar con backend para agregar
una migración `V20__posiciones_repartidor.sql` y un método en `TrackingServiceImpl`.

---

## 9. Snippet completo (Angular)

Servicio reutilizable:

```ts
// src/app/core/services/realtime.service.ts
import { Injectable, NgZone } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { BehaviorSubject, Subject } from 'rxjs';

import {
  PosicionBroadcastDto,
  EventoRutaWsDto,
  ErrorWsDto,
} from '../../core/models/realtime.models';

@Injectable({ providedIn: 'root' })
export class RealtimeService {
  private client?: Client;

  readonly conectado$ = new BehaviorSubject<boolean>(false);
  readonly errores$ = new Subject<ErrorWsDto>();

  constructor(private zone: NgZone) {}

  conectar(jwt: string): void {
    if (this.client?.active) return;

    this.client = new Client({
      webSocketFactory: () => new SockJS(`/ws?token=${encodeURIComponent(jwt)}`),
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      debug: () => {}, // silenciar logs en prod
    });

    this.client.onConnect = () => this.zone.run(() => this.conectado$.next(true));
    this.client.onDisconnect = () => this.zone.run(() => this.conectado$.next(false));

    this.client.activate();
  }

  desconectar(): void {
    this.client?.deactivate();
    this.conectado$.next(false);
  }

  suscribirseAPosiciones(rutaId: number, cb: (pos: PosicionBroadcastDto) => void): StompSubscription {
    return this.client!.subscribe(`/topic/rutas/${rutaId}/posiciones`, (msg: IMessage) => {
      this.zone.run(() => cb(JSON.parse(msg.body)));
    });
  }

  suscribirseAEventos(rutaId: number, cb: (evt: EventoRutaWsDto) => void): StompSubscription {
    return this.client!.subscribe(`/topic/rutas/${rutaId}/eventos`, (msg: IMessage) => {
      this.zone.run(() => cb(JSON.parse(msg.body)));
    });
  }

  suscribirseAErrores(): StompSubscription {
    return this.client!.subscribe('/user/queue/errors', (msg: IMessage) => {
      this.zone.run(() => this.errores$.next(JSON.parse(msg.body)));
    });
  }

  publicarPosicion(rutaId: number, pos: PosicionRepartidorDto): void {
    this.client!.publish({
      destination: `/app/rutas/${rutaId}/posicion`,
      body: JSON.stringify(pos),
    });
  }
}
```

Uso desde un componente admin:

```ts
@Component({
  selector: 'app-ruta-tracking',
  template: `<div #mapContainer class="mapa"></div>`,
})
export class RutaTrackingComponent implements OnDestroy {
  @Input() rutaId!: number;

  private sub?: StompSubscription;
  private watchId?: number;

  constructor(private realtime: RealtimeService, private map: MapService) {}

  ngOnInit(): void {
    this.realtime.conectar(this.auth.jwt);

    this.realtime.conectado$.pipe(filter(c => c)).subscribe(() => {
      this.sub = this.realtime.suscribirseAPosiciones(this.rutaId, pos => {
        this.map.moverRepartidor(pos);
      });
      this.realtime.suscribirseAErrores();
    });

    this.realtime.errores$.subscribe(err => this.toast.error(`${err.codigo}: ${err.mensaje}`));
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.realtime.desconectar();
  }
}
```

---

## 10. Snippet completo (Ionic / Capacitor / React Native)

En mobile nativo conviene usar el endpoint `/ws-native` (WebSocket puro):

```ts
import { Client } from '@stomp/stompjs';
// En RN: import { Client } from '@stomp/stompjs'; también funciona,
// pero sockjs-client no. Usá el WebSocket nativo de la plataforma.

const client = new Client({
  // En RN basta con brokerURL. El query param se concatena manualmente.
  brokerURL: `ws://api.host/ws-native?token=${encodeURIComponent(jwt)}`,
  // Si tu lib no permite query en brokerURL, pasá headers STOMP en connectHeaders
  // y mandá el token en el header Authorization (tambien soportado):
  connectHeaders: { Authorization: `Bearer ${jwt}` },
  reconnectDelay: 5000,
});

client.onConnect = () => {
  client.subscribe(`/topic/rutas/${rutaId}/posiciones`, msg => {
    const pos = JSON.parse(msg.body);
    // actualizar UI
  });
};

client.activate();
```

Para Capacitor con `@capacitor/geolocation` + STOMP nativo, el patrón es idéntico al de la
sección [Snippet Angular](#9-snippet-completo-angular), reemplazando `SockJS` por
`new WebSocket(...)`.

---

## 11. Pruebas manuales

### Con dos navegadores

1. **Repartidor** (login como REPARTIDOR, abrir `https://app.repartidor`).
   - Activar tracking → ver en consola del navegador:
     ```
     Sent: /app/rutas/10/posicion {latitud: -26.20, longitud: -58.21, ...}
     ```
2. **Admin** (login como ADMIN, abrir `https://admin.app/rutas/10`).
   - El mapa debe mostrar el marker actualizándose.

### Con `wscat` / `websocat`

```bash
# Terminal 1: login y obtener JWT
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"secret"}'

# Terminal 2: abrir sesion STOMP como admin y subscribirse
TOKEN=eyJhbGc...
wscat -c "ws://localhost:8080/ws-native?token=$TOKEN"
> {"command":"SUBSCRIBE","headers":{"destination":"/topic/rutas/10/posiciones"},"body":""}

# (en otra terminal) simular un repartidor publicando
TOKEN_R=eyJhbGc...
wscat -c "ws://localhost:8080/ws-native?token=$TOKEN_R"
> {"command":"SEND","headers":{"destination":"/app/rutas/10/posicion"},
   "body":"{\"latitud\":-26.20,\"longitud\":-58.21,
            \"timestampCliente\":\"2026-07-20T15:30:00Z\",\"origen\":\"GPS\"}"}

# Terminal 2 deberia imprimir algo asi:
< MESSAGE
  destination:/topic/rutas/10/posiciones
  content-type:application/json
  {"rutaId":10,"repartidorId":5,"repartidorNombre":"Juan Perez",
   "estadoRuta":"EN_CURSO","latitud":-26.20,"longitud":-58.21,
   "timestampCliente":"2026-07-20T15:30:00Z",
   "serverTimestamp":"2026-07-20T15:30:00.421Z",
   "origen":"GPS"}
```

### Casos a verificar

- [ ] Handshake falla con 401 cuando el token es basura.
- [ ] Handshake OK con token válido y rol REPARTIDOR.
- [ ] Repartidor publica → admin recibe el broadcast en <500 ms.
- [ ] Repartidor intenta subscribirse a la ruta de OTRO → no recibe nada (sub silenciosa falla).
- [ ] Admin se suscribe a `/topic/rutas/10/posiciones` y ve posiciones solo de la ruta 10.
- [ ] Enviar lat = 91 → llega `ErrorWsDto` con codigo `LATITUD_INVALIDA` a `/user/queue/errors`.
- [ ] Cerrar el WS del repartidor → no llegan más mensajes al admin.
- [ ] Reabrir la sesión STOMP después de un reconnect → las suscripciones se reestablecen automáticamente (re-suscribirse en `onConnect`).

---

## Referencias cruzadas

- **Contrato OpenAPI** (REST): `http://host:8080/swagger-ui.html` (autenticación, rutas, pedidos, stock, etc.).
- **Documentación interna del backend**: `README.md` → sección "Tracking en tiempo real (WebSocket / STOMP)".
- **Indicaciones generales del frontend**: `docs/INTEGRACION_FRONTEND.md`.
- **Repos / DTOs**: `src/main/java/com/sistemagas/pedidos/dto/realtime/*.java` — fuente de verdad.
