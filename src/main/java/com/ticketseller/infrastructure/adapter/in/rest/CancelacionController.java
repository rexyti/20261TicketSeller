package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.CancelarTicketUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CancelacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CancelarTicketRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class CancelacionController {

    private final CancelarTicketUseCase cancelarTicketUseCase;

    @PostMapping("/{id}/cancelar")
    public Mono<ResponseEntity<CancelacionResponse>> cancelarTicket(@PathVariable UUID id) {
        return cancelarTicketUseCase.ejecutar(List.of(id))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/cancelar-parcial")
    public Mono<ResponseEntity<CancelacionResponse>> cancelarParcial(@RequestBody CancelarTicketRequest request) {
        return cancelarTicketUseCase.ejecutar(request.ticketIds())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
