package com.ticketseller.application.postventa;

import com.ticketseller.domain.model.postventa.Reembolso;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CancelacionResultado(
        List<UUID> ticketsCancelados,
        UUID reembolsoId,
        BigDecimal montoPendiente,
        List<Reembolso> reembolsos
) {
}

