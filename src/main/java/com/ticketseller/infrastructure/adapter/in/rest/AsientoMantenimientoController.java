package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.mantenimiento.CambiarEstadoAsientoUseCase;
import com.ticketseller.application.mantenimiento.CambiarEstadoMasivoUseCase;
import com.ticketseller.application.mantenimiento.ConsultarHistorialAsientoUseCase;
import com.ticketseller.domain.model.Asiento;
import com.ticketseller.infrastructure.adapter.in.rest.dto.AsientoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoMasivoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoMasivoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.CambiarEstadoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.HistorialCambioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/eventos/{eventoId}/asientos")
@RequiredArgsConstructor
@Tag(name = "Mantenimiento de Asientos", description = "Endpoints para la gestión de estados y auditoría de asientos")
public class AsientoMantenimientoController {

    private final CambiarEstadoAsientoUseCase cambiarEstadoAsientoUseCase;
    private final CambiarEstadoMasivoUseCase cambiarEstadoMasivoUseCase;
    private final ConsultarHistorialAsientoUseCase consultarHistorialAsientoUseCase;

    @Operation(summary = "Cambiar el estado de un asiento individual")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Estado cambiado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Petición inválida"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Transición de estado inválida o asiento en proceso de compra")
    })
    @PatchMapping("/{asientoId}/estado")
    public Mono<ResponseEntity<AsientoResponse>> cambiarEstadoIndividual(
            @PathVariable UUID eventoId,
            @PathVariable UUID asientoId,
            @Valid @RequestBody CambiarEstadoRequest request) {
        
        // TODO: Extraer usuarioId del contexto de seguridad (usando un mock temporal)
        String usuarioId = "user-123";

        return cambiarEstadoAsientoUseCase.ejecutar(eventoId, asientoId, request.estadoDestino(), request.motivo(), usuarioId)
                .map(this::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Cambiar el estado de forma masiva a un lote de asientos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Operación masiva procesada exitosamente"),
            @ApiResponse(responseCode = "400", description = "Petición inválida")
    })
    @PatchMapping("/estado-masivo")
    public Mono<ResponseEntity<CambiarEstadoMasivoResponse>> cambiarEstadoMasivo(
            @PathVariable UUID eventoId,
            @Valid @RequestBody CambiarEstadoMasivoRequest request) {
        
        String usuarioId = "user-123";

        return cambiarEstadoMasivoUseCase.ejecutar(eventoId, request.asientoIds(), request.estadoDestino(), request.motivo(), usuarioId)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar el historial de auditoría de cambios de estado de un asiento")
    @ApiResponse(responseCode = "200", description = "Lista de historial retornada exitosamente")
    @GetMapping("/{asientoId}/historial")
    public Flux<HistorialCambioResponse> consultarHistorial(
            @PathVariable UUID eventoId,
            @PathVariable UUID asientoId) {
        
        return consultarHistorialAsientoUseCase.ejecutar(eventoId, asientoId)
                .map(h -> new HistorialCambioResponse(
                        h.getFechaHora(),
                        h.getUsuarioId(),
                        h.getEstadoAnterior(),
                        h.getEstadoNuevo(),
                        h.getMotivo()
                ));
    }

    private AsientoResponse toResponse(Asiento asiento) {
        return new AsientoResponse(
                asiento.getId(),
                asiento.getFila(),
                asiento.getColumna(),
                asiento.getNumero(),
                asiento.getZonaId(),
                asiento.getEstado()
        );
    }
}
