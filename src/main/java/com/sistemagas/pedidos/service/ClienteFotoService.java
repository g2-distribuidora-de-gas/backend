package com.sistemagas.pedidos.service;

import com.sistemagas.pedidos.dto.response.ClienteFotoResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

public interface ClienteFotoService {

    /**
     * Sube la primera foto de fachada del cliente.
     * Lanza 409 Conflict si el cliente ya tiene foto.
     */
    ClienteFotoResponse subirFoto(Long clienteId, MultipartFile archivo, String descripcion);

    /**
     * Reemplaza la foto existente del cliente.
     * Lanza 409 Conflict si el cliente no tiene foto previa.
     */
    ClienteFotoResponse reemplazarFoto(Long clienteId, MultipartFile archivo, String descripcion);

    /**
     * Sube una imagen como pendiente (flujo offline), bajo el path
     * "cliente-pending/{clienteId}/...". No se asocia al cliente hasta que
     * un pedido para ese cliente sea sincronizado.
     */
    String subirImagenPendiente(Long clienteId, MultipartFile archivo, String descripcion);

    /**
     * Llamado por SincronizacionPedidoProcessor despues de guardar un pedido.
     * Si el cliente no tiene foto y existe una imagen pendiente, la mueve a
     * "cliente-{id}/...", actualiza cliente.foto_evidencia_path y elimina la fila pendiente.
     */
    void asociarImagenACliente(Long clienteId);

    /**
     * Job de limpieza: borra las imagenes pendientes con uploadedAt anterior al cutoff
     * y los archivos huerfanos en Supabase.
     */
    void limpiarHuerfanos(Instant cutoff);
}
