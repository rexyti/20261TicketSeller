package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConfirmarOcupacionUseCase;
import com.ticketseller.application.ReservarAsientoUseCase;
import com.ticketseller.application.VerificarDisponibilidadUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.DisponibilidadResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final VerificarDisponibilidadUseCase verificarDisponibilidadUseCase;
    private final ReservarAsientoUseCase reservarAsientoUseCase;
    private final ConfirmarOcupacionUseCase confirmarOcupacionUseCase;
    private final com.ticketseller.application.LiberarAsientoUseCase liberarAsientoUseCase;

    public InventarioController(VerificarDisponibilidadUseCase verificarDisponibilidadUseCase,
                                ReservarAsientoUseCase reservarAsientoUseCase,
                                ConfirmarOcupacionUseCase confirmarOcupacionUseCase,
                                com.ticketseller.application.LiberarAsientoUseCase liberarAsientoUseCase) {
        this.verificarDisponibilidadUseCase = verificarDisponibilidadUseCase;
        this.reservarAsientoUseCase = reservarAsientoUseCase;
        this.confirmarOcupacionUseCase = confirmarOcupacionUseCase;
        this.liberarAsientoUseCase = liberarAsientoUseCase;
    }

    @GetMapping("/asientos/{id}/disponibilidad")
    public Mono<ResponseEntity<DisponibilidadResponse>> verificarDisponibilidad(@PathVariable UUID id) {
        return verificarDisponibilidadUseCase.ejecutar(id)
                .map(ResponseEntity::ok);
    }

    @PostMapping("/asientos/{id}/reservar")
    public Mono<ResponseEntity<DisponibilidadResponse>> reservarAsiento(
            @PathVariable UUID id, 
            @RequestBody com.ticketseller.infrastructure.adapter.in.rest.dto.ReservarAsientoRequest request) {
        return reservarAsientoUseCase.ejecutar(id, request.ventaId())
                .map(response -> ResponseEntity.status(201).body(response))
                .onErrorResume(com.ticketseller.domain.exception.AsientoReservadoPorOtroException.class, e -> 
                        Mono.just(ResponseEntity.status(409).body(new DisponibilidadResponse(id, false, e.getMessage()))));
    }

    @PostMapping("/asientos/{id}/ocupar")
    public Mono<ResponseEntity<Void>> ocuparAsiento(@PathVariable UUID id) {
        return confirmarOcupacionUseCase.ejecutar(id)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()));
    }

    @PostMapping("/asientos/{id}/liberar")
    public Mono<ResponseEntity<Void>> liberarAsiento(@PathVariable UUID id) {
        return liberarAsientoUseCase.ejecutar(id)
                .then(Mono.fromCallable(() -> ResponseEntity.ok().<Void>build()));
    }
}
