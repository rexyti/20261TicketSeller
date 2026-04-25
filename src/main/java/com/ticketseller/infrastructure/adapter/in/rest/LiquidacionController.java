package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.liquidacion.ConfigurarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarRecaudoIncrementalUseCase;
import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.CondicionTicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.ConfigurarModeloNegocioRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.ModeloNegocioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ApiErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LiquidacionController {

    private final ConsultarSnapshotUseCase consultarSnapshotUseCase;
    private final ConsultarModeloNegocioUseCase consultarModeloNegocioUseCase;
    private final ConfigurarModeloNegocioUseCase configurarModeloNegocioUseCase;
    private final ConsultarRecaudoIncrementalUseCase consultarRecaudoIncrementalUseCase;

    @GetMapping("/recintos/{id}/modelo-negocio")
    public Mono<ResponseEntity<ModeloNegocioResponse>> consultarModeloNegocio(@PathVariable UUID id) {
        return consultarModeloNegocioUseCase.ejecutar(id)
                .map(config -> new ModeloNegocioResponse(
                        config.getRecintoId(),
                        config.getModeloNegocio(),
                        config.getTipoRecinto(),
                        config.getMontoFijo()))
                .map(ResponseEntity::ok);
    }

    @PatchMapping("/recintos/{id}/modelo-negocio")
    public Mono<ResponseEntity<ModeloNegocioResponse>> configurarModeloNegocio(
            @PathVariable UUID id,
            @Valid @RequestBody ConfigurarModeloNegocioRequest request) {
        return configurarModeloNegocioUseCase.ejecutar(id, request.modelo(), request.montoFijo())
                .map(recinto -> new ModeloNegocioResponse(
                        recinto.getId(),
                        recinto.getModeloNegocio(),
                        recinto.getCategoria(),
                        recinto.getMontoFijo()))
                .map(ResponseEntity::ok);
    }

    @GetMapping("/eventos/{id}/snapshot")
    public Mono<ResponseEntity<SnapshotLiquidacionResponse>> consultarSnapshot(@PathVariable UUID id) {
        return consultarSnapshotUseCase.ejecutar(id)
                .map(snapshot -> {
                    var condiciones = snapshot.getCondiciones().values().stream()
                            .map(c -> new CondicionTicketResponse(c.getCondicion(), c.getCantidad(), c.getValorTotal()))
                            .toList();
                    return new SnapshotLiquidacionResponse(
                            snapshot.getEventoId(),
                            condiciones,
                            snapshot.getTimestampGeneracion());
                })
                .map(ResponseEntity::ok);
    }

    @GetMapping("/eventos/{id}/recaudo")
    public Mono<ResponseEntity<RecaudoIncrementalResponse>> consultarRecaudo(@PathVariable UUID id) {
        return consultarRecaudoIncrementalUseCase.ejecutar(id)
                .map(recaudo -> new RecaudoIncrementalResponse(
                        id,
                        recaudo.get("recaudoRegular"),
                        recaudo.get("recaudoCortesia"),
                        recaudo.get("cancelaciones"),
                        recaudo.get("recaudoNeto"),
                        LocalDateTime.now()))
                .map(ResponseEntity::ok);
    }
}
