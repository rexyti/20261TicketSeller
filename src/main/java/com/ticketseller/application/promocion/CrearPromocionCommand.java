package com.ticketseller.application.promocion;

import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;

import java.time.LocalDateTime;
import java.util.UUID;

public record CrearPromocionCommand(
        String nombre,
        TipoPromocion tipo,
        UUID eventoId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        TipoUsuario tipoUsuarioRestringido
) {
}

