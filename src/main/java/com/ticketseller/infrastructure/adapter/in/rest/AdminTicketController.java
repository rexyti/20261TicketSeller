package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.postventa.CambiarEstadoTicketUseCase;
import com.ticketseller.domain.repository.ReembolsoRepositoryPort;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoTicketRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketConReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.TicketRestMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final CambiarEstadoTicketUseCase cambiarEstadoTicketUseCase;
    private final ReembolsoRepositoryPort reembolsoRepositoryPort;
    private final TicketRestMapper mapper;

    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<TicketConReembolsoResponse>> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoTicketRequest request,
            @RequestHeader(value = "X-Agente-Id", required = false) UUID agenteId) {
        
        return cambiarEstadoTicketUseCase.ejecutar(id, request.estado(), request.justificacion(), agenteId)
                .flatMap(ticket -> reembolsoRepositoryPort.findByTicketId(ticket.getId())
                        .map(reembolso -> mapper.toResponse(ticket, reembolso))
                        .defaultIfEmpty(mapper.toResponse(ticket, null)))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
