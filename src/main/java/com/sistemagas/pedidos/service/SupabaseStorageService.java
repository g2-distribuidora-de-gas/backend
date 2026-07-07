package com.sistemagas.pedidos.service;

import org.springframework.web.multipart.MultipartFile;

public interface SupabaseStorageService {

    /**
     * Sube un archivo al bucket configurado de Supabase Storage y devuelve
     * la URL publica resultante.
     *
     * @param pedidoId      id del pedido asociado (se usa para organizar el object path).
     * @param archivo       MultipartFile con el contenido a subir.
     * @param descripcion   texto opcional para trazabilidad (no se persiste en Storage).
     * @return la URL publica donde queda el archivo.
     */
    String subir(Long pedidoId, MultipartFile archivo, String descripcion);

    /**
     * Elimina un archivo del bucket a partir de su URL publica.
     * Usado como compensacion cuando la operacion principal falla despues de subir.
     *
     * @param urlPublica URL publica devuelta por {@link #subir(Long, MultipartFile, String)}.
     */
    void eliminar(String urlPublica);
}