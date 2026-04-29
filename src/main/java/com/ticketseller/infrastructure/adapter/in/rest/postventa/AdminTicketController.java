package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.CambiarEstadoTicketUseCase;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.CambiarEstadoTicketRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.TicketConReembolsoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tickets")
@RequiredArgsConstructor
@Tag(name = "Postventa - Admin Tickets", description = "Cambio manual de estado de tickets")
public class AdminTicketController {
    private final CambiarEstadoTicketUseCase cambiarEstadoTicketUseCase;

    @Operation(summary = "Cambiar estado manual de ticket")
    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<TicketConReembolsoResponse>> cambiarEstado(@PathVariable UUID id,
                                                                           @Valid @RequestBody CambiarEstadoTicketRequest request) {
        return cambiarEstadoTicketUseCase.ejecutar(id, request.estado(), request.justificacion(), request.agenteId())
                .map(this::toResponse)
                .map(ResponseEntity::ok);
    }

    private TicketConReembolsoResponse toResponse(Ticket ticket) {
        return new TicketConReembolsoResponse(ticket.getId(), ticket.getEstado(), null, null, null);
    }
}

