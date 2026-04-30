package com.ticketseller.application.checkout;

import com.ticketseller.domain.model.promocion.TipoUsuario;

import java.util.UUID;

public record ReservarAsientosCommand(
        UUID compradorId,
        UUID eventoId,
        UUID zonaId,
        Integer cantidad,
        Boolean esCortesia,
        TipoUsuario tipoUsuario
) {
}

