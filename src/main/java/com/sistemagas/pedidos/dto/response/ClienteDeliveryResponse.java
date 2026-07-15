package com.sistemagas.pedidos.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Cliente destino del pedido en la vista del repartidor")
public class ClienteDeliveryResponse {

    @Schema(description = "ID del cliente", example = "7")
    private Long id;

    @Schema(description = "Nombre del cliente", example = "Juan Perez")
    private String nombre;

    @Schema(description = "Telefono del cliente", example = "+5491122334455")
    private String telefono;

    @Schema(description = "Direccion del cliente", example = "Av. Corrientes 1234, CABA")
    private String direccion;

    @Schema(description = "Latitud", example = "-34.6037")
    private BigDecimal latitud;

    @Schema(description = "Longitud", example = "-58.3816")
    private BigDecimal longitud;

    @Schema(description = "URL firmada de la foto de fachada del cliente. Null si no tiene o si Supabase fallo",
            example = "https://<supabase>/storage/v1/object/sign/...")
    private String urlFotoEvidencia;
}
