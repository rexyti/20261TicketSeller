package com.ticketseller.infrastructure.adapter.in.rest.asiento;

import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.application.inventario.VerificarDisponibilidadUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.DisponibilidadResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.AsientoRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventario/asientos")
@RequiredArgsConstructor
@Tag(name = "Inventario en Tiempo Real", description = "Endpoints para la gestión de disponibilidad y reservas de asientos")
public class InventarioController {

    private final VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;
    private final ConfirmarOcupacionUseCase confirmarOcupacionUseCase;
    private final AsientoRestMapper asientoRestMapper;

    @Operation(summary = "Verificar disponibilidad de un asiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación exitosa"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado")
    })
    @GetMapping("/{id}/disponibilidad")
    public Mono<ResponseEntity<DisponibilidadResponse>> verificarDisponibilidad(@PathVariable UUID id) {
        return verificarDisponibilidadUseCase.ejecutar(id)
                .map(asiento -> ResponseEntity.ok(asientoRestMapper.toDisponibilidadResponse(asiento)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Confirmar ocupación de un asiento tras pago exitoso")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asiento marcado como OCUPADO"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Hold expirado o asiento no en estado RESERVADO")
    })
    @PostMapping("/{id}/ocupar")
    public Mono<ResponseEntity<DisponibilidadResponse>> ocupar(@PathVariable UUID id) {
        return confirmarOcupacionUseCase.confirmar(id)
                .map(asiento -> ResponseEntity.ok(asientoRestMapper.toDisponibilidadResponse(asiento)));
    }

    @Operation(summary = "Liberar el hold de un asiento tras pago fallido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asiento liberado y disponible"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado")
    })
    @PostMapping("/{id}/liberar")
    public Mono<ResponseEntity<DisponibilidadResponse>> liberar(@PathVariable UUID id) {
        return confirmarOcupacionUseCase.liberar(id)
                .map(asiento -> ResponseEntity.ok(asientoRestMapper.toDisponibilidadResponse(asiento)));
    }
}
