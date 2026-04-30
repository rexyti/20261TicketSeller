package com.ticketseller.infrastructure.adapter.in.rest.conciliacion;

import com.ticketseller.application.conciliacion.ConfirmarTransaccionUseCase;
import com.ticketseller.application.conciliacion.ListarDiscrepanciaUseCase;
import com.ticketseller.application.conciliacion.ResolverDiscrepanciaUseCase;
import com.ticketseller.application.conciliacion.VerificarPagoUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.ConfirmarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.PagoResponse;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.ResolverDiscrepanciaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.conciliacion.dto.VerificarPagoRequest;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.ConciliacionRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Conciliación de Pagos", description = "Verificación, confirmación y resolución de discrepancias de pagos")
public class ConciliacionController {

    private final VerificarPagoUseCase verificarPagoUseCase;
    private final ConfirmarTransaccionUseCase confirmarTransaccionUseCase;
    private final ResolverDiscrepanciaUseCase resolverDiscrepanciaUseCase;
    private final ListarDiscrepanciaUseCase listarDiscrepanciaUseCase;
    private final ConciliacionRestMapper mapper;

    @Operation(summary = "Verificar pago de pasarela")
    @PostMapping("/api/v1/pagos/verificar")
    public Mono<ResponseEntity<PagoResponse>> verificar(@Valid @RequestBody VerificarPagoRequest request) {
        return verificarPagoUseCase.ejecutar(request.ventaId(), request.montoPasarela(), request.idExternoPasarela())
                .map(mapper::toPagoResponse)
                .map(r -> ResponseEntity.status(HttpStatus.CREATED).body(r));
    }

    @Operation(summary = "Confirmar transacción de forma idempotente")
    @PostMapping("/api/v1/pagos/confirmar")
    public Mono<ResponseEntity<PagoResponse>> confirmar(@Valid @RequestBody ConfirmarPagoRequest request) {
        return confirmarTransaccionUseCase.ejecutar(request.ventaId(), request.idExternoPasarela(), request.montoPasarela())
                .map(mapper::toPagoResponse)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Listar pagos en discrepancia")
    @GetMapping("/api/v1/admin/conciliacion/discrepancias")
    public Flux<PagoResponse> listarDiscrepancias() {
        return listarDiscrepanciaUseCase.ejecutar().map(mapper::toPagoResponse);
    }

    @Operation(summary = "Resolver una discrepancia de pago manualmente")
    @PatchMapping("/api/v1/admin/conciliacion/discrepancias/{id}/resolver")
    public Mono<ResponseEntity<PagoResponse>> resolver(
            @PathVariable UUID id,
            @Valid @RequestBody ResolverDiscrepanciaRequest request) {
        return resolverDiscrepanciaUseCase.ejecutar(id, request.confirmar(), request.agenteId(), request.justificacion())
                .map(mapper::toPagoResponse)
                .map(ResponseEntity::ok);
    }
}
