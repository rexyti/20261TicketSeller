package com.ticketseller.application.promocion;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearCodigosPromocionalesCommand(
        UUID promocionId,
        int cantidad,
        Integer usosMaximosPorCodigo,
        String prefijo,
        LocalDateTime fechaFin
) {
}

