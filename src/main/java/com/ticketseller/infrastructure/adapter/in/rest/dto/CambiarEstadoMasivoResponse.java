package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.util.List;

public record CambiarEstadoMasivoResponse(
        int modificados,
        int omitidos,
        List<String> mensajes
) {
}
