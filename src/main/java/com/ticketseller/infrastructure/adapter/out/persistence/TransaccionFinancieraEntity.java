package com.ticketseller.infrastructure.adapter.out.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("transacciones_financieras")
public class TransaccionFinancieraEntity {
    @Id
    private UUID id;
    private UUID ventaId;
    private BigDecimal monto;
    private String metodoPago;
    private String estadoPago;
    private String codigoAutorizacion;
    private String respuestaPasarela;
    private LocalDateTime fecha;
    private String ip;
}
