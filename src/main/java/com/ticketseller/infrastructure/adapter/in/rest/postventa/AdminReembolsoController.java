package com.ticketseller.infrastructure.adapter.in.rest.postventa;

import com.ticketseller.application.postventa.ConsultarColaReembolsosUseCase;
import com.ticketseller.application.postventa.ProcesarReembolsoManualUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoManualRequest;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoPendienteResponse;
import com.ticketseller.infrastructure.adapter.in.rest.postventa.dto.ReembolsoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.PostVentaRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Postventa - Admin Reembolsos", description = "Gestión manual y automática de reembolsos")
public class AdminReembolsoController {
    private final ProcesarReembolsoManualUseCase procesarReembolsoManualUseCase;
    private final ConsultarColaReembolsosUseCase consultarColaReembolsosUseCase;
    private final PostVentaRestMapper postVentaRestMapper;

    @Operation(summary = "Procesar reembolso manual por ticket")
    @PostMapping("/tickets/{id}/reembolso")
    public Mono<ResponseEntity<ReembolsoResponse>> procesarManual(@PathVariable UUID id,
                                                                  @Valid @RequestBody ReembolsoManualRequest request) {
        return procesarReembolsoManualUseCase.ejecutar(id, request.tipo(), request.monto(), request.agenteId())
                .map(postVentaRestMapper::toReembolsoResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Obtener cola de reembolsos pendientes")
    @GetMapping("/reembolsos/cola")
    public Flux<ReembolsoPendienteResponse> obtenerCola() {
        return consultarColaReembolsosUseCase.ejecutar()
                .map(postVentaRestMapper::toReembolsoPendienteResponse);
    }

    @Operation(summary = "Procesar cola automática de reembolsos pendientes")
    @PostMapping("/reembolsos/procesar-cola")
    public Mono<ResponseEntity<Void>> procesarCola() {
        return procesarReembolsoManualUseCase.procesarColaPendiente()
                .thenReturn(ResponseEntity.ok().build());
    }
}

