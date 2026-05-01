package com.ticketseller.domain.exception.asiento;

public class AsientoReservadoPorOtroException extends RuntimeException {
    public AsientoReservadoPorOtroException(String message) {
        super(message);
    }
}
