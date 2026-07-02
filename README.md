# Sistema de Gestión de Pedidos de Gas - Backend

Backend en **Spring Boot 3.3 + Java 17** para la gestión de pedidos de garrafas de gas.

## Arquitectura del Sistema

El sistema utiliza una arquitectura **Offline-First**, diseñada para operar aunque el usuario final pierda la conexión a internet de forma temporal.

- **Frontend (Cliente):** Responsable de detectar el estado de la red. Si no hay internet, guarda los pedidos localmente en el navegador (ej. IndexedDB) utilizando UUIDs generados en ese momento (`uuidOffline`). Cuando recupera la conexión, envía todos los pedidos pendientes en un solo lote al backend.
- **Backend (Servidor):** Desarrollado con **Spring Boot**. Procesa los pedidos y gestiona la base de datos central. Cuenta con un endpoint específico de sincronización masiva que resuelve conflictos y evita duplicados usando los `uuidOffline`.
- **Base de Datos:** **PostgreSQL 16**. El sistema soporta conectarse tanto a una base de datos local en Docker como a una en la nube (Supabase). Las migraciones estructurales se manejan mediante **Flyway**.

### Estructura Principal
- `controller/`: Endpoints REST expuestos para consumo del Frontend.
- `service/`: Lógica de negocio principal (sincronización, usuarios, garrafas).
- `repository/`: Interfaces de acceso a datos (Spring Data JPA).
- `dto/`: Objetos de transferencia (requests y responses).
- `model/`: Entidades JPA de la base de datos.

---

## Endpoints Principales

| Método | Endpoint | Descripción |
|---|---|---|
| `POST` | `/api/pedidos` | Crear pedido individual (online) |
| `GET`  | `/api/pedidos` | Listar todos los pedidos |
| `GET`  | `/api/pedidos/{id}` | Obtener el detalle de un pedido por su ID |
| `POST` | `/api/garrafas` | Registrar una nueva garrafa en el sistema |
| `GET`  | `/api/garrafas` | Listar el catálogo de garrafas |
| `POST` | `/api/usuarios` | Registrar un nuevo usuario (cliente o admin) |
| `GET`  | `/api/usuarios` | Listar usuarios |
| `POST` | `/api/sincronizar` | **Sincronizar lote de pedidos creados offline** (Punto clave de la arquitectura) |
| `GET`  | `/swagger-ui.html` | Documentación interactiva y panel de pruebas de la API (Swagger) |

---

## Cómo levantar el proyecto

El proyecto está preparado para ejecutarse de dos maneras distintas, permitiendo cambiar fácilmente entre una **base de datos local** y una **base de datos en la nube**. 

Todo esto se controla a través del archivo `.env` situado en la raíz del proyecto, modificando la variable `SPRING_PROFILES_ACTIVE`.

### Opción A: Base de Datos Local (Perfil `dev`)
*Ideal para testear sin internet o trabajar en desarrollo sin afectar los datos reales.*

1. Modifica tu archivo `.env` para activar el perfil de desarrollo:
   ```env
   SPRING_PROFILES_ACTIVE=dev
   ```
2. Levanta la base de datos local usando el archivo `docker-compose.yml` provisto:
   ```bash
   docker-compose up -d
   ```
3. Levanta la aplicación Spring Boot:
   ```bash
   mvn spring-boot:run
   ```
> **Nota:** La aplicación se conectará al contenedor de Docker (`localhost:5432`) y Flyway creará las tablas automáticamente.

### Opción B: Base de Datos en la Nube (Perfil `prod`)
*Para conectar la aplicación a la base de datos de producción alojada en Supabase.*

1. Modifica tu archivo `.env` para activar el perfil de producción y asegúrate de tener las credenciales correctas:
   ```env
   SPRING_PROFILES_ACTIVE=prod
   SUPABASE_PROJECT_REF=tu_project_ref
   SUPABASE_DB_PASSWORD=tu_password
   SUPABASE_DB_URL=jdbc:postgresql://tu_host_de_supabase...
   ```
2. Levanta la aplicación Spring Boot:
   ```bash
   mvn spring-boot:run
   ```
> **Nota:** La aplicación ignorará el contenedor de Docker y se conectará directamente a los servidores de Supabase en internet.