package com.ticketseller.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public class Ticket {
    private final UUID id;
    private final UUID ventaId;
    private final UUID eventoId;
    private final UUID zonaId;
    private final UUID compuertaId;
    private final String codigoQR;
    private final EstadoTicket estado;
    private final BigDecimal precio;
    private final boolean esCortesia;

    public Ticket(UUID id, UUID ventaId, UUID eventoId, UUID zonaId, UUID compuertaId,
                  String codigoQR, EstadoTicket estado, BigDecimal precio, boolean esCortesia) {
        this.id = id;
        this.ventaId = ventaId;
        this.eventoId = eventoId;
        this.zonaId = zonaId;
        this.compuertaId = compuertaId;
        this.codigoQR = codigoQR;
        this.estado = estado;
        this.precio = precio;
        this.esCortesia = esCortesia;
    }

    public UUID getId() { return id; }
    public UUID getVentaId() { return ventaId; }
    public UUID getEventoId() { return eventoId; }
    public UUID getZonaId() { return zonaId; }
    public UUID getCompuertaId() { return compuertaId; }
    public String getCodigoQR() { return codigoQR; }
    public EstadoTicket getEstado() { return estado; }
    public BigDecimal getPrecio() { return precio; }
    public boolean isEsCortesia() { return esCortesia; }

    public Ticket withEstado(EstadoTicket nuevoEstado) {
        return new Ticket(id, ventaId, eventoId, zonaId, compuertaId, codigoQR, nuevoEstado, precio, esCortesia);
    }

    public Ticket withCodigoQR(String qr) {
        return new Ticket(id, ventaId, eventoId, zonaId, compuertaId, qr, estado, precio, esCortesia);
    }
}
