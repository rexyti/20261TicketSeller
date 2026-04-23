package com.ticketseller.infrastructure.adapter.out.persistence.checkout;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table("transacciones_financieras")
public class TransaccionFinancieraEntity {
    @Id
    private UUID id;
    @Column("venta_id")
    private UUID ventaId;
    private BigDecimal monto;
    @Column("metodo_pago")
    private String metodoPago;
    @Column("estado_pago")
    private String estadoPago;
    @Column("codigo_autorizacion")
    private String codigoAutorizacion;
    @Column("respuesta_pasarela")
    private String respuestaPasarela;
    private LocalDateTime fecha;
    private String ip;
}

