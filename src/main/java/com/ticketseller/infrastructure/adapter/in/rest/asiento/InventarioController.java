package com.ticketseller.infrastructure.adapter.in.rest.asiento;

import com.ticketseller.application.inventario.AsientoEnEvento;
import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.application.inventario.LiberarHoldUseCase;
import com.ticketseller.application.inventario.ObtenerInventarioEventoUseCase;
import com.ticketseller.application.inventario.VerificarDisponibilidadUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.DisponibilidadResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.InventarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eventos/{eventoId}/inventario")
@RequiredArgsConstructor
@Tag(name = "Inventario en Tiempo Real", description = "Endpoints para la gestión de disponibilidad y reservas de asientos")
public class InventarioController {

    private final VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;
    private final ConfirmarOcupacionUseCase confirmarOcupacionUseCase;
    private final LiberarHoldUseCase liberarHoldUseCase;
    private final ObtenerInventarioEventoUseCase obtenerInventarioEventoUseCase;

    @Operation(summary = "Obtener inventario de asientos de un evento con su estado por evento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Inventario obtenido exitosamente")
    })
    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GESTOR_INVENTARIO', 'PROMOTOR_EVENTOS', 'AGENTE_VENTAS')")
    public Flux<InventarioResponse> obtenerInventario(@PathVariable UUID eventoId) {
        return obtenerInventarioEventoUseCase.ejecutar(eventoId)
                .map(this::toInventarioResponse);
    }

    @Operation(summary = "Verificar disponibilidad de un asiento para un evento específico")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación exitosa"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado")
    })
    @GetMapping("/{id}/disponibilidad")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GESTOR_INVENTARIO', 'AGENTE_VENTAS')")
    public Mono<ResponseEntity<DisponibilidadResponse>> verificarDisponibilidad(
            @PathVariable UUID eventoId, @PathVariable UUID id) {
        return verificarDisponibilidadUseCase.ejecutar(id, eventoId)
                .map(ae -> ResponseEntity.ok(toDisponibilidadResponse(ae)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Confirmar ocupación de un asiento tras pago exitoso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asiento marcado como VENDIDO"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Hold expirado o no existente para este evento")
    })
    @PatchMapping("/{id}/ocupar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GESTOR_INVENTARIO')")
    public Mono<ResponseEntity<DisponibilidadResponse>> ocupar(
            @PathVariable UUID eventoId, @PathVariable UUID id) {
        return confirmarOcupacionUseCase.ejecutar(id, eventoId)
                .map(hold -> ResponseEntity.ok(new DisponibilidadResponse(hold.getAsientoId(), hold.getEstado().name())));
    }

    @Operation(summary = "Liberar el hold de un asiento tras pago fallido o cancelación")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Hold liberado, asiento disponible"),
            @ApiResponse(responseCode = "404", description = "Hold no encontrado para este evento")
    })
    @PatchMapping("/{id}/liberar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'GESTOR_INVENTARIO')")
    public Mono<ResponseEntity<DisponibilidadResponse>> liberar(
            @PathVariable UUID eventoId, @PathVariable UUID id) {
        return liberarHoldUseCase.ejecutar(id, eventoId)
                .map(hold -> ResponseEntity.ok(new DisponibilidadResponse(hold.getAsientoId(), AsientoEnEvento.DISPONIBLE)));
    }

    private InventarioResponse toInventarioResponse(AsientoEnEvento ae) {
        return new InventarioResponse(ae.asientoId(), ae.numero(), ae.zonaId(), ae.estadoEnEvento());
    }

    private DisponibilidadResponse toDisponibilidadResponse(AsientoEnEvento ae) {
        return new DisponibilidadResponse(ae.asientoId(), ae.estadoEnEvento());
    }
}
