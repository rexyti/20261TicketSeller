package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.GestionarReembolsoManualUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ReembolsoManualRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ReembolsoResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class AdminReembolsoController {

    private final GestionarReembolsoManualUseCase gestionarReembolsoManualUseCase;

    @PostMapping("/{id}/reembolso")
    public Mono<ResponseEntity<ReembolsoResponse>> procesarReembolso(
            @PathVariable UUID id,
            @Valid @RequestBody ReembolsoManualRequest request,
            @RequestHeader(value = "X-Agente-Id", required = false) UUID agenteId) {
        
        return gestionarReembolsoManualUseCase.ejecutar(id, request.tipo(), agenteId)
                .map(r -> new ReembolsoResponse(
                        r.getId(),
                        r.getEstado(),
                        r.getMonto(),
                        r.getAgenteId(),
                        r.getFechaCompletado()
                ))
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/reembolsos/procesar-cola")
    public Mono<ResponseEntity<Void>> procesarCola() {
        return gestionarReembolsoManualUseCase.procesarCola()
                .thenReturn(ResponseEntity.ok().build());
    }
}
