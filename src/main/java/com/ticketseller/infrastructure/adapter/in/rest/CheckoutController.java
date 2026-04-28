package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.checkout.ConsultarVentaUseCase;
import com.ticketseller.application.checkout.ProcesarPagoUseCase;
import com.ticketseller.application.checkout.ReservarAsientosUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.checkout.VentaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CheckoutRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

@Tag(name = "Checkout", description = "Reserva y pago de tickets")
@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final ReservarAsientosUseCase reservarAsientosUseCase;
    private final ProcesarPagoUseCase procesarPagoUseCase;
    private final ConsultarVentaUseCase consultarVentaUseCase;
    private final CheckoutRestMapper checkoutRestMapper;

    @Operation(summary = "Reservar asientos para un evento")
    @PostMapping("/reservar")
    public Mono<ResponseEntity<VentaResponse>> reservar(@Valid @RequestBody ReservarAsientosRequest request) {
        return reservarAsientosUseCase.ejecutar(checkoutRestMapper.toCommand(request))
                .map(checkoutRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Procesar el pago de una reserva activa")
    @PostMapping("/{ventaId}/pagar")
    public Mono<ResponseEntity<VentaResponse>> pagar(@PathVariable UUID ventaId,
                                                     @Valid @RequestBody ProcesarPagoRequest request) {
        return procesarPagoUseCase.ejecutar(ventaId, checkoutRestMapper.toCommand(request))
                .map(checkoutRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar el estado de una venta")
    @GetMapping("/{ventaId}")
    public Mono<ResponseEntity<VentaResponse>> consultar(@PathVariable UUID ventaId) {
        return consultarVentaUseCase.ejecutar(ventaId)
                .map(checkoutRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}

