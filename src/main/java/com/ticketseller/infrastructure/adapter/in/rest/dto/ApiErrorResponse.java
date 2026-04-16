package com.ticketseller.infrastructure.adapter.in.rest.dto;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        String codigo,
        String mensaje,
        LocalDateTime timestamp
) {
}

