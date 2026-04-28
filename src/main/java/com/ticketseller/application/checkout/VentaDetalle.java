package com.ticketseller.application.checkout;

import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;

import java.util.List;

public record VentaDetalle(Venta venta, List<Ticket> tickets) {
}

