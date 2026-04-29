package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.GestionarReembolsoManualUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.ReembolsoManualRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.postventa.ReembolsoResponse;
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
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Postventa - Admin Reembolsos", description = "Gestión manual y automática de reembolsos")
public class AdminReembolsoController {
    private final GestionarReembolsoManualUseCase gestionarReembolsoManualUseCase;
    private final PostVentaRestMapper postVentaRestMapper;

    @Operation(summary = "Procesar reembolso manual por ticket")
    @PostMapping("/tickets/{id}/reembolso")
    public Mono<ResponseEntity<ReembolsoResponse>> procesarManual(@PathVariable UUID id,
                                                                  @Valid @RequestBody ReembolsoManualRequest request) {
        return gestionarReembolsoManualUseCase.ejecutar(id, request.tipo(), request.monto(), request.agenteId())
                .map(postVentaRestMapper::toReembolsoResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Procesar cola automática de reembolsos pendientes")
    @PostMapping("/reembolsos/procesar-cola")
    public Mono<ResponseEntity<Void>> procesarCola() {
        return gestionarReembolsoManualUseCase.procesarColaPendiente()
                .thenReturn(ResponseEntity.ok().build());
    }
}

