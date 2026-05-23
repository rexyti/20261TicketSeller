package com.ticketseller.application.promocion;

import com.ticketseller.domain.model.promocion.Descuento;

import java.util.UUID;

public record CodigoValidado(UUID codigoId, Descuento descuento) {
}
