package com.ticketseller.application.checkout;

import java.util.List;
import java.util.UUID;

public record ReservarAsientosCommand(
        UUID compradorId,
        UUID eventoId,
        UUID zonaId,
        Integer cantidad,
        Boolean esCortesia,
        List<UUID> asientoIds
) {
}

