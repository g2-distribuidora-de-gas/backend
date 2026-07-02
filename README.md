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

Copiar `.env.example` a `.env` y completar:

```bash
SUPABASE_PROJECT_REF=abcdefghijkl
SUPABASE_DB_PASSWORD=tu-password-real
SPRING_PROFILES_ACTIVE=prod
```

### 3. Conexion

El backend se conecta al endpoint **Direct** (puerto 5432) de Supabase:

```
jdbc:postgresql://db.<project_ref>.supabase.co:5432/postgres
```

**Importante:** Supabase requiere SSL. La URL ya lo incluye por configuracion.

Flyway ejecutara las migraciones automaticamente al primer arranque.

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