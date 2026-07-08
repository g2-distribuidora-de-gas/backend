# Instructivo: cambiar el bucket de Supabase Storage a privado

Prerrequisito: ser `Owner` o `Admin` del proyecto Supabase.

## Pasos

1. **Ir al dashboard de Supabase**
   - Abrir https://app.supabase.com y seleccionar el proyecto donde esta el bucket.

2. **Navegar a Storage**
   - En el menu lateral izquierdo, click en **Storage** (icono de cubo).

3. **Abrir la configuracion del bucket**
   - En la lista de buckets, buscar `pedidos-evidencia`.
   - Click en el bucket para abrir el detalle.
   - Click en el icono de **configuracion** (engranaje, arriba a la derecha) o en **Configuration**.

4. **Cambiar la visibilidad a Private**
   - En la seccion **General** o **Settings**, aparece un toggle **Public bucket**.
   - **Desactivarlo** (toggle en OFF).
   - Aparece una confirmacion; aceptar.

5. **Verificar que cambio**
   - El bucket ahora muestra un badge **Private** (en vez de Public).
   - Si se intenta acceder a una URL publica vieja (`https://xxx.supabase.co/storage/v1/object/public/...`), debe devolver **400 Bad Request** o **403 Forbidden**.

6. **Verificar las variables de entorno del backend**
   - En el `.env` o config del backend de dev, asegurarse de tener:
     ```
     SUPABASE_STORAGE_ENABLED=true
     SUPABASE_PROJECT_REF=<tu-project-ref>
     SUPABASE_SERVICE_ROLE_KEY=<tu-service-role-key>
     SUPABASE_STORAGE_BUCKET=pedidos-evidencia
     SUPABASE_STORAGE_PUBLIC_BASE_URL=        # dejar vacio o borrar
     SUPABASE_SIGNED_URL_TTL=3600            # 1 hora
     ```
   - El `service-role-key` esta en **Settings -> API -> service_role** (es secreto, NO el `anon` key).

7. **Smoke test del backend**
   - Levantar el backend y probar:
     ```
     POST /api/clientes/1/foto  (multipart con un jpg)
     GET  /api/clientes/1       (debe devolver urlFotoEvidencia con signed URL)
     ```
   - Abrir la signed URL en el browser: debe mostrar la imagen.
   - Esperar 1 hora y volver a abrir la misma URL: debe dar error de expiracion (confirma que la firma funciona).

## Cosas a tener en cuenta

- **Las URLs publicas viejas dejan de funcionar inmediatamente.** Si hay algun lugar donde estan hardcodeadas, hay que migrar a signed URLs.
- **El frontend tiene que estar listo para pedir el pedido cada vez que necesita ver la imagen** (porque la signed URL expira). Una estrategia comun es cachear la respuesta del pedido/cliente por un tiempo menor al TTL.
- **El job de limpieza corre a las 3am** (`@Scheduled(cron = "0 0 3 * * *")`), asi que cualquier imagen huerfana subida durante el dia se limpia al dia siguiente.
- El backend ya esta preparado para el bucket privado desde V11: `SupabaseStorageService` genera signed URLs con `getSignedUrl(...)` y el `SupabaseStorageProperties.signedUrlTtlSeconds` controla el TTL (default 3600s = 1h).
