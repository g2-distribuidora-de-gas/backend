# Sistema de Gestion de Pedidos de Gas - Backend

Backend en **Spring Boot 3.3 + Java 17** para gestion de pedidos de garrafas de gas con sincronizacion offline-first.

## Caracteristicas

- API REST para gestion de pedidos, garrafas y usuarios
- Sincronizacion offline-first con idempotencia (via `uuidOffline`)
- Resolucion de conflictos con estrategia **server-wins**
- PostgreSQL como base de datos (Docker local / Supabase en nube)
- Flyway para migraciones
- Documentacion Swagger/OpenAPI

## Stack tecnologico

| Componente | Tecnologia |
|---|---|
| Framework | Spring Boot 3.3 |
| Lenguaje | Java 17 |
| Build | Maven |
| Persistencia | Spring Data JPA + Hibernate |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway |
| Mapeo DTO | MapStruct |
| Docs | springdoc-openapi |
| Logs | Logback + SLF4J |

## Requisitos

- Java 17+
- Maven 3.9+
- Docker + Docker Compose (para entorno dev)

## Arranque rapido (desarrollo local)

### 1. Levantar base de datos local

```bash
docker compose up -d
```

Esto levanta PostgreSQL 16 en `localhost:5432` con DB `pedidos_gas`.

### 2. Compilar y correr

```bash
mvn clean install
mvn spring-boot:run
```

La aplicacion arrancara en `http://localhost:8080` con perfil `dev` por defecto.

### 3. Verificar

- API: `http://localhost:8080/api/pedidos`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health: `http://localhost:8080/actuator/health`

## Configuracion para Supabase (produccion)

### 1. Crear proyecto en Supabase

1. Crear cuenta en [supabase.com](https://supabase.com)
2. Crear nuevo proyecto
3. Ir a **Project Settings > Database**
4. Copiar:
   - **Project Reference** (ej: `abcdefghijkl`)
   - **Database Password** (la que definiste al crear el proyecto)

### 2. Configurar variables de entorno

Editar el archivo `.env` en la raiz del proyecto y completar:

```bash
SUPABASE_PROJECT_REF=tu-project-ref
SUPABASE_PASS=tu-password
SUPABASE_DB_USER=postgres.tu-project-ref
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/postgres?sslmode=require&prepareThreshold=0
SPRING_PROFILES_ACTIVE=prod
```

**Variables requeridas:**

| Variable | Ejemplo | Descripcion |
|----------|---------|-------------|
| `SUPABASE_PROJECT_REF` | `mqklsftjyesuiazsmuin` | ID del proyecto en Supabase |
| `SUPABASE_PASS` | (la definida al crear el proyecto) | Password de la DB |
| `SUPABASE_DB_USER` | `postgres.mqklsftjyesuiazsmuin` | Usuario completo |
| `SUPABASE_DB_URL` | `jdbc:postgresql://aws-1-ca-central-1.pooler.supabase.com:5432/postgres?sslmode=require&prepareThreshold=0` | JDBC URL completa |
| `JWT_SECRET` | (random >=64 chars) | Clave firma JWT. **Rechaza arrancar en PROD si es placeholder o < 64 chars** |
| `DEPOSITO_LAT` | `-26.2072404` | Latitud del deposito (default Formosa). Usado como origen de las rutas optimizadas |
| `DEPOSITO_LNG` | `-58.2123249` | Longitud del deposito (default Formosa) |

### Validaciones al arranque (PROD)

Al levantar la aplicacion con `SPRING_PROFILES_ACTIVE=prod`, se ejecutan validaciones previas
(`EnvironmentPostProcessor`). Si alguna falla, la aplicacion **no arranca** y muestra
mensajes claros en stderr:

- **SupabaseConfigValidator**:
  - `SUPABASE_PROJECT_REF`, `SUPABASE_PASS`, `SUPABASE_DB_USER`, `SUPABASE_DB_URL` no pueden
    estar vacios ni tener valores placeholder (`undefined`, `tu-project-ref`, etc.)
  - Si `app.supabase.storage.enabled=true`: tambien exige `SUPABASE_SERVICE_ROLE_KEY` y bucket
- **SecurityConfigValidator**:
  - `JWT_SECRET` no puede estar vacio
  - `JWT_SECRET` no puede empezar con `clave-secreta-de-desarrollo` (placeholder de dev)
  - `JWT_SECRET` debe tener al menos 64 caracteres
  - Genera una clave segura con: `openssl rand -base64 64`

En perfil `dev` ninguna de estas validaciones se ejecuta (podes usar placeholders).

### 3. Conexion

El backend se conecta al endpoint **Pooler (Supavisor transaction mode)**. Hay dos opciones:

- **Pooler (recomendado para Supabase nube):** `aws-1-ca-central-1.pooler.supabase.com:5432`
  - Requiere `prepareThreshold=0` en la URL JDBC
  - Mejor para conexiones desde la nube
- **Direct:** `db.<project_ref>.supabase.co:5432`
  - Conexion directa al PostgreSQL

**Importante:** Supabase requiere SSL (`sslmode=require`). La URL ya lo incluye.

### 4. Primer deploy (reset del esquema)

Si la base de datos ya tiene tablas de un deploy anterior y Flyway choca al arrancar
(`relation "pedidos" already exists`), ejecutar el script de reset:

**Windows (PowerShell):**
```powershell
.\scripts\reset-db.ps1
```

**Linux / Mac:**
```bash
./scripts/reset-db.sh
```

**Manual (psql):**
```bash
psql "$SUPABASE_DB_URL" -f src/main/resources/db/admin/reset_schema.sql
```

> El script de reset BORRA todas las tablas y la tabla `flyway_schema_history`.
> Solo para setup inicial o para un reset completo. **No usar en produccion con datos reales.**

Una vez ejecutado el reset, arrancar la app y Flyway creara las tablas desde cero:

```bash
mvn spring-boot:run
```

Deberia verse en el log:
```
Configuracion de Supabase validada correctamente (PROD)
  Project ref: ...
  Modo: Supavisor Pooler (transaction mode)
SupabasePool - Start completed.
Flyway: Successfully applied 2 migrations
```

## Estructura del proyecto

```
src/main/java/com/sistemagas/pedidos/
  config/         Configuraciones tecnicas (CORS, Swagger, Async, etc.)
  controller/     Endpoints REST
  service/        Logica de negocio (interfaces + impl)
  dto/            Objetos de transferencia (request/response)
  mapper/         Conversion DTO <-> Entity (MapStruct)
  model/          Entidades JPA + base/Auditable
  enums/          Enums del dominio (estados, tipos)
  repository/     Repositorios Spring Data JPA
  specification/  Queries dinamicas JPA
  exception/      Excepciones + handler global
  validation/     Validadores custom
  util/           Utilidades
```

## Endpoints principales

> La documentacion completa y actualizada esta disponible en Swagger UI:
> `http://localhost:8080/swagger-ui.html`

### Autenticacion

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `POST` | `/api/auth/login` | Login con email y password | publico |
| `POST` | `/api/auth/register` | Registrar nuevo usuario | ADMIN, SUPER_ADMIN |

### Usuarios

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `GET` | `/api/usuarios` | Listar usuarios | ADMIN, SUPER_ADMIN |
| `POST` | `/api/usuarios` | Crear usuario | ADMIN, SUPER_ADMIN |
| `DELETE` | `/api/usuarios/{id}` | Desactivar usuario (baja logica) | ADMIN, SUPER_ADMIN |
| `PATCH` | `/api/usuarios/{id}/reactivar` | Reactivar usuario | ADMIN, SUPER_ADMIN |

### Clientes

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `POST` | `/api/clientes` | Crear cliente | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `GET` | `/api/clientes` | Listar clientes | cualquier autenticado |
| `GET` | `/api/clientes/{id}` | Obtener cliente por ID | cualquier autenticado |
| `PUT` | `/api/clientes/{id}` | Actualizar cliente | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `DELETE` | `/api/clientes/{id}` | Eliminar cliente (baja logica) | ADMIN, SUPER_ADMIN |
| `POST` | `/api/clientes/{id}/foto` | Subir primera foto de fachada | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `PUT` | `/api/clientes/{id}/foto` | Reemplazar foto de fachada | PREVENTISTA, ADMIN, SUPER_ADMIN |

### Pedidos

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `POST` | `/api/pedidos` | Crear pedido individual (online) | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `GET` | `/api/pedidos` | Listar pedidos | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `GET` | `/api/pedidos/{id}` | Obtener pedido por ID | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `GET` | `/api/pedidos/creador/{creadorId}` | Listar pedidos por creador | PREVENTISTA, REPARTIDOR, ADMIN, SUPER_ADMIN |
| `GET` | `/api/pedidos/uuid/{uuidOffline}` | Buscar pedido por UUID offline | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `PATCH` | `/api/pedidos/{id}/estado` | Actualizar estado de un pedido | REPARTIDOR, ADMIN, SUPER_ADMIN |
| `POST` | `/api/pedidos/{id}/foto` | **DEPRECATED** Subir foto al pedido (migrar a `/api/clientes/{id}/foto`) | PREVENTISTA, REPARTIDOR, ADMIN, SUPER_ADMIN |

### Garrafas

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `GET` | `/api/garrafas` | Listar garrafas del catalogo | cualquier autenticado |
| `POST` | `/api/garrafas` | Crear garrafa | ADMIN, SUPER_ADMIN |
| `PUT` | `/api/garrafas/{id}` | Actualizar garrafa | ADMIN, SUPER_ADMIN |
| `PATCH` | `/api/garrafas/{id}/precio` | Actualizar precio de garrafa | ADMIN, SUPER_ADMIN |
| `POST` | `/api/garrafas/{id}/reposicion` | Reponer stock de garrafa | ADMIN, SUPER_ADMIN |

### Sincronizacion offline

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `POST` | `/api/sincronizar` | Sincronizar lote de pedidos offline | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `GET` | `/api/sincronizar/estado?uuids=...` | Consultar UUIDs ya procesados | PREVENTISTA, ADMIN, SUPER_ADMIN |
| `POST` | `/api/sincronizar/clientes/imagenes` | Subir imagen pendiente de cliente (offline) | PREVENTISTA, ADMIN, SUPER_ADMIN |

### Rutas de reparto

| Metodo | Endpoint | Descripcion | Roles |
|---|---|---|---|
| `POST` | `/api/rutas/planificar` | Planificar una ruta de reparto | ADMIN, SUPER_ADMIN |
| `GET` | `/api/rutas/mis-rutas/{repartidorId}` | Obtener la ruta activa de un repartidor | REPARTIDOR, ADMIN, SUPER_ADMIN |
| `PATCH` | `/api/rutas/{rutaId}/estado` | Cambiar el estado de una ruta | REPARTIDOR, ADMIN, SUPER_ADMIN |
| `PATCH` | `/api/rutas/paradas/{rutaPedidoId}` | Actualizar estado de una parada | REPARTIDOR, ADMIN, SUPER_ADMIN |

## Auditoria automatica

Todas las entidades que heredan de `Auditable` (`clientes`, `pedidos`, `pedido_detalles`,
`garrafas`, `usuarios`, `rutas`) registran automaticamente:

- **`creadoPor`**: email del usuario autenticado al momento del INSERT, o `"SYSTEM"` si no
  hay contexto de seguridad (seeds, jobs batch, scripts)
- **`actualizadoPor`**: email del usuario autenticado al momento del UPDATE
- **`createdAt`**, **`updatedAt`**: timestamps UTC gestionados por JPA Auditing

Los campos se exponen en los responses de las APIs. Esto es posible gracias a
`SecurityAuditorAware`, que extrae el principal del `SecurityContextHolder` (el email del
JWT). No requiere ninguna accion manual en services ni controllers.

## Codigos de error

Todas las respuestas de error devuelven un `ApiResponse` con `data.codigo` para que el
cliente identifique el tipo de error sin parsear el mensaje:

| Codigo | HTTP | Cuando |
|---|---|---|
| `USUARIO_NO_ENCONTRADO` | 404 | Token JWT valido pero el usuario no existe en BD |
| `NO_AUTHENTICATED` | 401 | No hay contexto de seguridad (sin token / token invalido) |
| `GARRAFA_TIPO_OBLIGATORIO` | 400 | Garrafa sin `tipo` |
| `GARRAFA_CAPACIDAD_OBLIGATORIA` | 400 | Garrafa sin `capacidadKg` |
| `GARRAFA_CAPACIDAD_INCONSISTENTE` | 400 | `capacidadKg` no coincide con el tipo declarado |
| `PEDIDO_DUPLICADO` | 400 | `uuidOffline` ya existe |

Y los genericos:

| Codigo | HTTP | Cuando |
|---|---|---|
| `BUSINESS_ERROR` | 400 | Cualquier `BusinessException` sin codigo especifico |

## Sincronizacion offline

Flujo recomendado para clientes (Angular con IndexedDB):

1. **Sin conexion:** cliente guarda pedidos en IndexedDB con `uuidOffline` generado localmente
2. **Recupera conexion:** cliente hace `GET /api/sincronizar/estado?uuids=...` para saber que ya esta procesado
3. **Manda pendientes:** cliente hace `POST /api/sincronizar` con la lista de pedidos pendientes
4. **Procesa respuesta:** servidor devuelve 4 listas: `exitosos`, `duplicados`, `conflictos`, `fallidos`
5. **Actualiza IndexedDB:** cliente actualiza cada pedido con su ID real del servidor

### Ejemplo de peticion de sincronizacion

```json
POST /api/sincronizar
{
  "clienteFecha": "2024-01-15T10:30:00Z",
  "pedidos": [
    {
      "uuidOffline": "abc-123",
      "clienteId": 1,
      "garrafaId": 2,
      "cantidad": 1,
      "direccionEntrega": "Calle Falsa 123",
      "clienteFechaCreacion": "2024-01-15T10:25:00Z"
    }
  ]
}
```

### Ejemplo de respuesta

```json
{
  "mensaje": "Sincronizacion procesada",
  "total": 1,
  "exitosos": [
    {
      "uuidOffline": "abc-123",
      "id": 42,
      "estado": "PENDIENTE"
    }
  ],
  "duplicados": [],
  "conflictos": [],
  "fallidos": []
}
```

## Tests

```bash
mvn test
```

## Evidencia visual (foto de fachada)

La foto de fachada se asocia al **cliente** (no al pedido). Asi un cliente tiene UNA sola
foto persistente, sin importar cuantos pedidos tenga. El backend actua como **proxy seguro**
entre el cliente y Supabase Storage, exponiendo endpoints REST que suben el archivo y
devuelven una URL firmada (expira en 1h por default).

### Endpoints canonicos

| Metodo | Endpoint | Uso |
|---|---|---|
| `POST` | `/api/clientes/{id}/foto` | Subir la **primera** foto (falla con 409 si ya tiene) |
| `PUT` | `/api/clientes/{id}/foto` | **Reemplazar** la foto existente (falla con 409 si no tiene) |
| `POST` | `/api/sincronizar/clientes/imagenes` | Subir imagen pendiente (flujo offline) |

### Flujo online

1. El cliente envia `multipart/form-data` a `POST /api/clientes/{id}/foto` con el campo
   `archivo` (la imagen) y opcionalmente `descripcion` (texto libre).
2. El backend valida tipo (jpg/png/webp) y tamano (default 10MB).
3. Se sube al bucket de Supabase Storage usando la `service_role_key` (bypasea RLS).
4. La URL firmada devuelta se guarda en la columna `clientes.foto_evidencia_path`.
5. La respuesta incluye `clienteId`, `urlFotoEvidencia`, `nombreArchivo`, `contentType` y
   `tamanioBytes`. La URL es **firmada** (no publica) y expira en 1h.

### Flujo offline

Ver [docs/INTEGRACION_FRONTEND.md](./docs/INTEGRACION_FRONTEND.md) seccion C.

### Endpoint DEPRECATED

`POST /api/pedidos/{id}/foto` existe pero esta **deprecado**. Migrar a los endpoints de
cliente arriba. Se mantiene por compatibilidad hacia atras pero sera removido en la
proxima major version.

### Ejemplo con cURL

```bash
curl -X POST http://localhost:8080/api/clientes/7/foto \
  -H "Authorization: Bearer <JWT>" \
  -F "archivo=@/path/fachada.jpg" \
  -F "descripcion=Fachada principal"
```

### Variables de entorno

```bash
SUPABASE_STORAGE_ENABLED=true
SUPABASE_STORAGE_PROJECT_REF=mqklsftjyesuiazsmuin  # opcional si defines PUBLIC_BASE_URL
SUPABASE_SERVICE_ROLE_KEY=<service-role-jwt>        # NO anon key (la mas privilegiada)
SUPABASE_STORAGE_BUCKET=pedidos-evidencia
SUPABASE_STORAGE_PUBLIC_BASE_URL=                   # opcional, se infiere del project ref
SUPABASE_STORAGE_MAX_FILE_SIZE_BYTES=10485760       # 10MB
```

> **Importante:** nunca expongas la `SUPABASE_SERVICE_ROLE_KEY` al frontend. El backend la
> usa internamente para bypasear RLS al subir archivos al bucket. Configurala solo en el
> `.env` del backend.

### Crear el bucket en Supabase

La primera vez, crear el bucket publico desde la consola o por SQL:

```sql
-- Crear bucket publico para evidencias
insert into storage.buckets (id, name, public)
values ('pedidos-evidencia', 'pedidos-evidencia', true)
on conflict (id) do nothing;

-- Policy publica de lectura para todos (los uploads los hace el backend con service_role)
create policy "lectura publica de evidencias"
on storage.objects for select
using ( bucket_id = 'pedidos-evidencia' );
```

### Limites multipart

Configurados en `application.yml` (sobrescribibles por perfil):

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 12MB
```

Si el archivo excede el limite, el backend devuelve `413 Payload Too Large`. Si el tipo
no es permitido, devuelve `400 Bad Request`.

## Build para produccion

```bash
mvn clean package -DskipTests
java -jar target/pedidos-backend.jar
```

O con Docker:

```bash
docker build -t gas-pedidos-backend .
docker run -p 8080:8080 --env-file .env gas-pedidos-backend
```

## Licencia

MIT
