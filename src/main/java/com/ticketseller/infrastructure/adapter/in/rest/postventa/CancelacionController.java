package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.CancelarTicketUseCase;
import com.ticketseller.application.postventa.ProcesarReembolsoMasivoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.CancelacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.CancelarTicketRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Postventa - Cancelaciones", description = "Cancelación de tickets por comprador")
public class CancelacionController {
    private final CancelarTicketUseCase cancelarTicketUseCase;
    private final ProcesarReembolsoMasivoUseCase procesarReembolsoMasivoUseCase;
    private final PostVentaRestMapper postVentaRestMapper;

    @Operation(summary = "Cancelar ticket individual")
    @PostMapping("/{id}/cancelar")
    public Mono<ResponseEntity<CancelacionResponse>> cancelar(@PathVariable UUID id) {
        return cancelarTicketUseCase.cancelarTicket(id)
                .map(postVentaRestMapper::toCancelacionResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Cancelar tickets parcialmente")
    @PostMapping("/cancelar-parcial")
    public Mono<ResponseEntity<CancelacionResponse>> cancelarParcial(@Valid @RequestBody CancelarTicketRequest request) {
        return cancelarTicketUseCase.cancelarVarios(request.ticketIds())
                .map(postVentaRestMapper::toCancelacionResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Procesar cancelación masiva por evento cancelado")
    @PostMapping("/eventos/{eventoId}/cancelar")
    public Mono<ResponseEntity<Void>> cancelarEvento(@PathVariable UUID eventoId) {
        return procesarReembolsoMasivoUseCase.ejecutar(eventoId)
                .thenReturn(ResponseEntity.ok().build());
    }
}

