package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.ConsultarVentaUseCase;
import com.ticketseller.application.LiberarReservaUseCase;
import com.ticketseller.application.ProcesarPagoUseCase;
import com.ticketseller.application.ReservarAsientosUseCase;
import com.ticketseller.domain.exception.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.PagoRechazadoException;
import com.ticketseller.domain.exception.ReservaExpiradaException;
import com.ticketseller.domain.exception.VentaNotFoundException;
import com.ticketseller.infrastructure.adapter.in.rest.dto.VentaDtoMapper;
import com.ticketseller.infrastructure.adapter.in.rest.dto.TicketDtoMapper;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.VentaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final ReservarAsientosUseCase reservarAsientosUseCase;
    private final ProcesarPagoUseCase procesarPagoUseCase;
    private final LiberarReservaUseCase liberarReservaUseCase;
    private final ConsultarVentaUseCase consultarVentaUseCase;
    private final VentaDtoMapper ventaDtoMapper;
    private final TicketDtoMapper ticketDtoMapper;

    @PostMapping("/reservar")
    public Mono<ResponseEntity<VentaResponse>> reservarAsientos(@RequestBody ReservarAsientosRequest request) {
        log.info("Solicitud de reserva para evento {}, {} tickets", request.eventoId(), request.ticketIds().size());
        
        return reservarAsientosUseCase.ejecutar(
                        request.eventoId(),
                        request.compradorId(),
                        request.ticketIds(),
                        request.total())
                .map(venta -> ResponseEntity.status(HttpStatus.CREATED).body(ventaDtoMapper.toResponse(venta)))
                .onErrorResume(AsientoNoDisponibleException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT).build()));
    }

    @PostMapping("/{ventaId}/pagar")
    public Mono<ResponseEntity<Object>> procesarPago(
            @PathVariable UUID ventaId,
            @RequestBody ProcesarPagoRequest request) {
        log.info("Solicitud de pago para venta {}, método: {}", ventaId, request.metodoPago());

        return procesarPagoUseCase.ejecutar(ventaId, request.metodoPago())
                .<ResponseEntity<Object>>map(venta -> ResponseEntity.ok(ventaDtoMapper.toResponse(venta)))
                .onErrorResume(PagoRechazadoException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                                .body(Map.of("error", "Pago rechazado", "mensaje", e.getMessage()))))
                .onErrorResume(ReservaExpiradaException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("error", "Reserva expirada", "mensaje", e.getMessage()))))
                .onErrorResume(VentaNotFoundException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(Map.of("error", "Venta no encontrada", "mensaje", e.getMessage()))))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just((ResponseEntity<Object>) ResponseEntity.status(HttpStatus.CONFLICT)
                                .body((Object) Map.of("error", "Estado inválido", "mensaje", e.getMessage()))));
    }

    @GetMapping("/{ventaId}")
    public Mono<ResponseEntity<Map<String, Object>>> consultarVenta(@PathVariable UUID ventaId) {
        log.info("Consultando venta {}", ventaId);

        return consultarVentaUseCase.ejecutar(ventaId)
                .map(detalle -> ResponseEntity.ok(Map.of(
                        "venta", ventaDtoMapper.toResponse(detalle.venta()),
                        "tickets", detalle.tickets().stream()
                                .map(ticketDtoMapper::toResponse)
                                .toList()
                )))
                .onErrorResume(VentaNotFoundException.class, e ->
                        Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build()));
    }

    @PostMapping("/liberar-expiradas")
    public Mono<ResponseEntity<Void>> liberarReservasExpiradas() {
        log.info("Solicitud de liberación de reservas expiradas");
        
        return liberarReservaUseCase.ejecutar()
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}
