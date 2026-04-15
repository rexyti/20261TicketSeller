package com.ticketseller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TransaccionFinanciera {
    private final UUID id;
    private final UUID ventaId;
    private final BigDecimal monto;
    private final String metodoPago;
    private final String estadoPago;
    private final String codigoAutorizacion;
    private final String respuestaPasarela;
    private final LocalDateTime fecha;
    private final String ip;

    public TransaccionFinanciera(UUID id, UUID ventaId, BigDecimal monto, String metodoPago,
                                 String estadoPago, String codigoAutorizacion, String respuestaPasarela,
                                 LocalDateTime fecha, String ip) {
        this.id = id;
        this.ventaId = ventaId;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.estadoPago = estadoPago;
        this.codigoAutorizacion = codigoAutorizacion;
        this.respuestaPasarela = respuestaPasarela;
        this.fecha = fecha;
        this.ip = ip;
    }

    public UUID getId() { return id; }
    public UUID getVentaId() { return ventaId; }
    public BigDecimal getMonto() { return monto; }
    public String getMetodoPago() { return metodoPago; }
    public String getEstadoPago() { return estadoPago; }
    public String getCodigoAutorizacion() { return codigoAutorizacion; }
    public String getRespuestaPasarela() { return respuestaPasarela; }
    public LocalDateTime getFecha() { return fecha; }
    public String getIp() { return ip; }
}
