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

| Metodo | Endpoint | Descripcion |
|---|---|---|
| `POST` | `/api/pedidos` | Crear pedido individual |
| `GET` | `/api/pedidos` | Listar pedidos |
| `GET` | `/api/pedidos/{id}` | Obtener pedido por ID |
| `PATCH` | `/api/pedidos/{id}/estado` | Actualizar estado de un pedido |
| `POST` | `/api/pedidos/{id}/foto` | **Subir foto de fachada como evidencia visual** |
| `GET` | `/api/pedidos/uuid/{uuidOffline}` | Buscar pedido por UUID offline |
| `POST` | `/api/garrafas` | Crear garrafa |
| `GET` | `/api/garrafas` | Listar garrafas |
| `POST` | `/api/usuarios` | Registrar usuario |
| `GET` | `/api/usuarios` | Listar usuarios |
| `POST` | `/api/sincronizar` | **Sincronizar pedidos offline** |
| `GET` | `/api/sincronizar/estado?uuids=...` | Consultar UUIDs ya procesados |
| `GET` | `/swagger-ui.html` | Documentacion interactiva |

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
      "usuarioId": 1,
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

Cada pedido puede llevar asociada una foto de fachada como evidencia visual. El backend
actua como **proxy seguro** entre el cliente y Supabase Storage, exponiendo un unico
endpoint REST que sube el archivo, lo persiste y devuelve la URL publica.

### Flujo

1. El cliente envia `multipart/form-data` al endpoint `POST /api/pedidos/{id}/foto` con el
   campo `archivo` (la imagen) y opcionalmente `descripcion` (texto libre).
2. El backend valida tipo (jpg/png/webp) y tamano (default 10MB).
3. Se sube al bucket de Supabase Storage usando la `service_role_key` (bypasea RLS).
4. La URL publica devuelta se guarda en la columna `pedidos.url_foto_evidencia`.
5. La respuesta incluye `pedidoId`, `urlFotoEvidencia`, `nombreArchivo`, `contentType` y `tamanioBytes`.

### Ejemplo con cURL

```bash
curl -X POST http://localhost:8080/api/pedidos/42/foto \
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
