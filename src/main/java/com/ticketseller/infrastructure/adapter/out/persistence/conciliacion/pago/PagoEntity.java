package com.ticketseller.infrastructure.adapter.out.persistence.conciliacion.pago;

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
@Table("pagos")
public class PagoEntity {
    @Id
    private UUID id;
    @Column("venta_id")
    private UUID ventaId;
    @Column("id_externo_pasarela")
    private String idExternoPasarela;
    @Column("monto_esperado")
    private BigDecimal montoEsperado;
    @Column("monto_pasarela")
    private BigDecimal montoPasarela;
    private String estado;
    @Column("agente_id")
    private UUID agenteId;
    @Column("justificacion_resolucion")
    private String justificacionResolucion;
    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;
    @Column("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
