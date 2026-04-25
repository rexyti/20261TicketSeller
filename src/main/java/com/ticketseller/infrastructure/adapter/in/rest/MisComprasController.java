package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConsultarEstadoReembolsoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketConReembolsoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/compras")
@RequiredArgsConstructor
public class MisComprasController {

    private final ConsultarEstadoReembolsoUseCase consultarEstadoReembolsoUseCase;

    @GetMapping("/mis-compras")
    public Flux<TicketConReembolsoResponse> listarMisCompras(
            @RequestHeader("X-Comprador-Id") UUID compradorId) {
        return consultarEstadoReembolsoUseCase.ejecutar(compradorId);
    }
}
