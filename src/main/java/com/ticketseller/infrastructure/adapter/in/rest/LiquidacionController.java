package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.liquidacion.ConfigurarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarRecaudoIncrementalUseCase;
import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.ConfigurarModeloNegocioRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.ModeloNegocioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.LiquidacionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.util.UUID;

@Tag(name = "Liquidación", description = "Consulta de liquidación y recaudo de eventos")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LiquidacionController {

    private final ConsultarSnapshotUseCase consultarSnapshotUseCase;
    private final ConsultarModeloNegocioUseCase consultarModeloNegocioUseCase;
    private final ConfigurarModeloNegocioUseCase configurarModeloNegocioUseCase;
    private final ConsultarRecaudoIncrementalUseCase consultarRecaudoIncrementalUseCase;
    private final LiquidacionRestMapper liquidacionRestMapper;

    @Operation(summary = "Consultar el modelo de negocio de un recinto")
    @GetMapping("/recintos/{id}/modelo-negocio")
    public Mono<ResponseEntity<ModeloNegocioResponse>> consultarModeloNegocio(@PathVariable UUID id) {
        return consultarModeloNegocioUseCase.ejecutar(id)
                .map(liquidacionRestMapper::toModeloNegocioResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Configurar el modelo de negocio de un recinto")
    @PatchMapping("/recintos/{id}/modelo-negocio")
    public Mono<ResponseEntity<ModeloNegocioResponse>> configurarModeloNegocio(
            @PathVariable UUID id,
            @Valid @RequestBody ConfigurarModeloNegocioRequest request) {
        return configurarModeloNegocioUseCase.ejecutar(id, request.modelo(), request.montoFijo())
                .map(liquidacionRestMapper::toModeloNegocioResponseFromRecinto)
                .map(ResponseEntity::ok);
    }

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
