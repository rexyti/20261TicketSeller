package com.ticketseller.infrastructure.adapter.in.rest.bloqueos.dto;

import com.ticketseller.domain.model.bloqueos.CategoriaCortesia;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CrearCortesiaGeneralRequest(
        @NotBlank String destinatario,
        @NotNull CategoriaCortesia categoria,
        UUID zonaId
) {
}
