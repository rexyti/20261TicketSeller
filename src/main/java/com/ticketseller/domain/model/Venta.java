package com.ticketseller.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Venta {
    private final UUID id;
    private final UUID compradorId;
    private final UUID eventoId;
    private final EstadoVenta estado;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaExpiracion;
    private final BigDecimal total;

    public Venta(UUID id, UUID compradorId, UUID eventoId, EstadoVenta estado,
                 LocalDateTime fechaCreacion, LocalDateTime fechaExpiracion, BigDecimal total) {
        this.id = id;
        this.compradorId = compradorId;
        this.eventoId = eventoId;
        this.estado = estado;
        this.fechaCreacion = fechaCreacion;
        this.fechaExpiracion = fechaExpiracion;
        this.total = total;
    }

    public UUID getId() { return id; }
    public UUID getCompradorId() { return compradorId; }
    public UUID getEventoId() { return eventoId; }
    public EstadoVenta getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public BigDecimal getTotal() { return total; }

    public boolean estaExpirada(LocalDateTime ahora) {
        return fechaExpiracion != null && ahora.isAfter(fechaExpiracion);
    }

    public Venta withEstado(EstadoVenta nuevoEstado) {
        return new Venta(id, compradorId, eventoId, nuevoEstado, fechaCreacion, fechaExpiracion, total);
    }
}
