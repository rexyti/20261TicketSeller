package com.ticketseller.infrastructure.adapter.in.rest.liquidacion;

import com.ticketseller.application.liquidacion.ConsultarRecaudoIncrementalUseCase;
import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.LiquidacionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Liquidación", description = "Consulta de liquidación y recaudo de eventos")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LiquidacionController {

    private final ConsultarSnapshotUseCase consultarSnapshotUseCase;
    private final ConsultarRecaudoIncrementalUseCase consultarRecaudoIncrementalUseCase;
    private final LiquidacionRestMapper liquidacionRestMapper;

    @Operation(summary = "Consultar snapshot de liquidación de un evento")
    @GetMapping("/eventos/{id}/snapshot")
    public Mono<ResponseEntity<SnapshotLiquidacionResponse>> consultarSnapshot(@PathVariable UUID id) {
        return consultarSnapshotUseCase.ejecutar(id)
                .map(liquidacionRestMapper::toSnapshotResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar recaudo incremental de un evento")
    @GetMapping("/eventos/{id}/recaudo")
    public Mono<ResponseEntity<RecaudoIncrementalResponse>> consultarRecaudo(@PathVariable UUID id) {
        return consultarRecaudoIncrementalUseCase.ejecutar(id)
                .map(recaudo -> liquidacionRestMapper.toRecaudoResponse(id, recaudo))
                .map(ResponseEntity::ok);
    }
}
