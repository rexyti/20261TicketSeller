package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearCortesiaGeneralRequest(
        @NotNull UUID destinatarioId,
        String emailDestinatario,
        @NotNull CategoriaCortesia categoria,
        UUID zonaId
) {
}
