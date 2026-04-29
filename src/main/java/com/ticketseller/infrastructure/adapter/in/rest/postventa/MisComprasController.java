package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.ConsultarEstadoReembolsoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.TicketConReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Tag(name = "Postventa - Mis Compras", description = "Consulta del estado de reembolsos del comprador")
public class MisComprasController {
    private final ConsultarEstadoReembolsoUseCase consultarEstadoReembolsoUseCase;
    private final PostVentaRestMapper postVentaRestMapper;

    @Operation(summary = "Consultar tickets cancelados/reembolsados del comprador autenticado")
    @GetMapping("/mis-compras")
    public Flux<TicketConReembolsoResponse> misCompras(@RequestHeader("X-Comprador-Id") UUID compradorId) {
        return consultarEstadoReembolsoUseCase.ejecutar(compradorId)
                .map(postVentaRestMapper::toTicketConReembolsoResponse);
    }
}

