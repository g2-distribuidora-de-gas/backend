package com.sistemagas.pedidos.model;

import com.sistemagas.pedidos.enums.EstadoPedido;
import com.sistemagas.pedidos.model.base.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "pedidos",
        indexes = {
                @Index(name = "idx_pedido_uuid_offline", columnList = "uuid_offline"),
                @Index(name = "idx_pedido_estado", columnList = "estado")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(max = 100)
    @Column(name = "uuid_offline", unique = true, length = 100)
    private String uuidOffline;

    @NotNull(message = "El cliente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @NotBlank(message = "La direccion de entrega es obligatoria")
    @Size(max = 300)
    @Column(name = "direccion_entrega", nullable = false, length = 300)
    private String direccionEntrega;

    @NotNull(message = "El estado es obligatorio")
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedido estado;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PedidoDetalle> detalles = new ArrayList<>();

    @Version
    private Long version;

    public void agregarDetalle(PedidoDetalle detalle) {
        detalles.add(detalle);
        detalle.setPedido(this);
    }
}