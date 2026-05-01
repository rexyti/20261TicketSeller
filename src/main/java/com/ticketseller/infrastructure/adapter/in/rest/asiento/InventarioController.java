package com.ticketseller.infrastructure.adapter.in.rest.asiento;

import com.ticketseller.application.inventario.ConfirmarOcupacionUseCase;
import com.ticketseller.application.inventario.ReservarAsientoUseCase;
import com.ticketseller.application.inventario.VerificarDisponibilidadUseCase;
import com.ticketseller.domain.model.asiento.Asiento;
import com.ticketseller.domain.model.asiento.EstadoAsiento;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.DisponibilidadResponse;
import com.ticketseller.infrastructure.adapter.in.rest.asiento.dto.ReservarAsientoRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventario/asientos")
@RequiredArgsConstructor
@Tag(name = "Inventario en Tiempo Real", description = "Endpoints para la gestión de disponibilidad y reservas de asientos")
public class InventarioController {

    private final VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;
    private final ReservarAsientoUseCase reservarAsientoUseCase;
    private final ConfirmarOcupacionUseCase confirmarOcupacionUseCase;

    @Operation(summary = "Verificar disponibilidad de un asiento")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Verificación exitosa"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado")
    })
    @GetMapping("/{id}/disponibilidad")
    public Mono<ResponseEntity<DisponibilidadResponse>> verificarDisponibilidad(@PathVariable UUID id) {
        return verificarDisponibilidadUseCase.ejecutar(id)
                .map(asiento -> ResponseEntity.ok(toDisponibilidadResponse(asiento)))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Reservar un asiento con hold temporal de 15 minutos")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asiento reservado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado"),
            @ApiResponse(responseCode = "409", description = "Asiento no disponible")
    })
    @PostMapping("/{id}/reservar")
    public Mono<ResponseEntity<DisponibilidadResponse>> reservar(
            @PathVariable UUID id,
            @RequestBody ReservarAsientoRequest request) {
        return reservarAsientoUseCase.ejecutar(id)
                .map(asiento -> ResponseEntity.status(HttpStatus.CREATED)
                        .body(toDisponibilidadResponse(asiento)));
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
                .map(asiento -> ResponseEntity.ok(toDisponibilidadResponse(asiento)));
    }

    @Operation(summary = "Liberar el hold de un asiento tras pago fallido")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asiento liberado y disponible"),
            @ApiResponse(responseCode = "404", description = "Asiento no encontrado")
    })
    @PostMapping("/{id}/liberar")
    public Mono<ResponseEntity<DisponibilidadResponse>> liberar(@PathVariable UUID id) {
        return confirmarOcupacionUseCase.liberar(id)
                .map(asiento -> ResponseEntity.ok(toDisponibilidadResponse(asiento)));
    }

    private DisponibilidadResponse toDisponibilidadResponse(Asiento asiento) {
        boolean disponible = EstadoAsiento.DISPONIBLE.equals(asiento.getEstado());
        String mensaje = disponible ? null : "ASIENTO NO DISPONIBLE";
        return new DisponibilidadResponse(
                asiento.getId(),
                disponible,
                asiento.getEstado().name(),
                asiento.getExpiraEn(),
                mensaje);
    }
}
