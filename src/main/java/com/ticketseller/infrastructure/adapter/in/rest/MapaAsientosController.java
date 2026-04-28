package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.asiento.CrearMapaAsientosUseCase;
import com.ticketseller.application.asiento.MarcarEspacioVacioUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.AsientoMapaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.tipoasiento.CrearMapaAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AsientoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Tag(name = "Mapa de Asientos", description = "Gestión del mapa de asientos de un recinto")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recintos")
public class MapaAsientosController {

    private final CrearMapaAsientosUseCase crearMapaAsientosUseCase;
    private final MarcarEspacioVacioUseCase marcarEspacioVacioUseCase;
    private final AsientoRestMapper asientoRestMapper;

    @Operation(summary = "Crear mapa de asientos NxM para un recinto")
    @PostMapping("/{recintoId}/mapa")
    public Flux<AsientoMapaResponse> crearMapa(@PathVariable UUID recintoId,
                                               @Valid @RequestBody CrearMapaAsientosRequest request) {
        return crearMapaAsientosUseCase.ejecutar(recintoId, request.filas(), request.columnasPorFila())
                .map(asientoRestMapper::toResponse);
    }

    @Operation(summary = "Marcar un asiento como espacio vacío")
    @PatchMapping("/{recintoId}/mapa/asientos/{asientoId}")
    public Mono<ResponseEntity<AsientoMapaResponse>> marcarEspacioVacio(@PathVariable UUID recintoId,
                                                                        @PathVariable UUID asientoId) {
        return marcarEspacioVacioUseCase.ejecutar(asientoId)
                .map(asientoRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
