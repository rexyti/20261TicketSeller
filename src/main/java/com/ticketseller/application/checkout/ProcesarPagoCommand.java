package com.ticketseller.application.checkout;

public record ProcesarPagoCommand(
        String metodoPago,
        String ip
) {
}

