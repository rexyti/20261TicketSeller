package com.ticketseller.application.checkout;

import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;

import java.util.List;

public record VentaDetalle(Venta venta, List<Ticket> tickets) {
}

