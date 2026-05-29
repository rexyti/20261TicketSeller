package com.ticketseller.infrastructure.adapter.in.rest.checkout;

import com.ticketseller.application.checkout.ConsultarVentaUseCase;
import com.ticketseller.application.checkout.ProcesarPagoUseCase;
import com.ticketseller.application.checkout.ReservarAsientosUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.ProcesarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.ReservarAsientosRequest;
import com.ticketseller.infrastructure.adapter.in.rest.checkout.dto.VentaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.CheckoutRestMapper;
import com.ticketseller.infrastructure.config.SecurityContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CheckoutController {

    private final ReservarAsientosUseCase reservarAsientosUseCase;
    private final ProcesarPagoUseCase procesarPagoUseCase;
    private final ConsultarVentaUseCase consultarVentaUseCase;
    private final CheckoutRestMapper checkoutRestMapper;

    @Operation(summary = "Reservar asientos para un evento")
    @PostMapping("/eventos/{eventoId}/asientos/reservar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPRADOR', 'AGENTE_VENTAS')")
    public Mono<ResponseEntity<VentaResponse>> reservar(@PathVariable UUID eventoId,
                                                        @Valid @RequestBody ReservarAsientosRequest request) {
        return SecurityContextUtils.getUsuarioId()
                .flatMap(compradorId -> reservarAsientosUseCase.ejecutar(
                        checkoutRestMapper.toCommand(request, eventoId, compradorId)))
                .map(checkoutRestMapper::toResponse)
                .map(response -> ResponseEntity.status(HttpStatus.CREATED).body(response));
    }

    @Operation(summary = "Procesar el pago de una reserva activa")
    @PostMapping("/checkout/{ventaId}/pagar")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPRADOR', 'AGENTE_VENTAS')")
    public Mono<ResponseEntity<VentaResponse>> pagar(@PathVariable UUID ventaId,
                                                     @Valid @RequestBody ProcesarPagoRequest request) {
        return procesarPagoUseCase.ejecutar(ventaId, checkoutRestMapper.toCommand(request))
                .map(checkoutRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar el estado de una venta")
    @GetMapping("/checkout/{ventaId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPRADOR', 'AGENTE_VENTAS')")
    public Mono<ResponseEntity<VentaResponse>> consultar(@PathVariable UUID ventaId) {
        return consultarVentaUseCase.ejecutar(ventaId)
                .map(checkoutRestMapper::toResponse)
                .map(ResponseEntity::ok);
    }
}
