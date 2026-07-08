package com.sistemagas.pedidos.service;

import org.springframework.web.multipart.MultipartFile;

public interface SupabaseStorageService {

    /**
     * Sube un archivo al bucket configurado de Supabase Storage bajo el prefijo indicado.
     *
     * @param prefijoPath   prefijo de organizacion (ej: "cliente-42", "cliente-pending/7")
     * @param archivo       MultipartFile con el contenido a subir.
     * @param descripcion   texto opcional para trazabilidad (no se persiste en Storage).
     * @return el object path resultante (ej: "cliente-42/abc-uuid.jpg").
     */
    String subir(String prefijoPath, MultipartFile archivo, String descripcion);

    /**
     * Sube un archivo organizandolo bajo "pedido-{pedidoId}".
     *
     * @deprecated desde V11 la foto se asocia al cliente, no al pedido.
     *             Mantenido temporalmente para compatibilidad de la app mobile.
     */
    @Deprecated
    String subir(Long pedidoId, MultipartFile archivo, String descripcion);

    /**
     * Elimina un archivo del bucket a partir de su object path.
     *
     * @param objectPath path dentro del bucket (ej: "cliente-42/abc-uuid.jpg")
     */
    void eliminar(String objectPath);

    /**
     * Genera una URL firmada (signed URL) para acceder a un archivo del bucket privado.
     * Usa el TTL configurado en app.supabase.storage.signed-url-ttl-seconds.
     *
     * @param objectPath path dentro del bucket
     * @return URL firmada con expiracion
     */
    String getSignedUrl(String objectPath);

    /**
     * Genera una URL firmada con TTL custom.
     */
    String getSignedUrl(String objectPath, int ttlSeconds);

    /**
     * Mueve un archivo dentro del bucket (rename). Usado para promover imagenes
     * de "cliente-pending/{id}/..." a "cliente-{id}/..." una vez asociado al cliente.
     */
    void mover(String sourcePath, String destPath);
}