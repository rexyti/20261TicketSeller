package com.ticketseller.infrastructure.adapter.in.rest.liquidacion;

import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.liquidacion.dto.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.LiquidacionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final LiquidacionRestMapper liquidacionRestMapper;

    @Operation(summary = "Consultar snapshot de liquidación de un evento")
    @GetMapping("/eventos/{id}/snapshot")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMINISTRADOR_FINANCIERO')")
    public Mono<ResponseEntity<SnapshotLiquidacionResponse>> consultarSnapshot(@PathVariable UUID id) {
        return consultarSnapshotUseCase.ejecutar(id)
                .map(liquidacionRestMapper::toSnapshotResponse)
                .map(ResponseEntity::ok);
    }
}
