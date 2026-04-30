package com.ticketseller.infrastructure.adapter.in.rest.transaccion;

import com.ticketseller.application.transaccion.CambiarEstadoVentaUseCase;
import com.ticketseller.application.transaccion.ConsultarHistorialVentaUseCase;
import com.ticketseller.application.transaccion.FiltroTransacciones;
import com.ticketseller.application.transaccion.ListarTransaccionesUseCase;
import com.ticketseller.domain.model.venta.EstadoVenta;
import com.ticketseller.infrastructure.adapter.in.rest.mapper.TransaccionRestMapper;
import com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto.CambiarEstadoVentaRequest;
import com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto.HistorialEstadoVentaResponse;
import com.ticketseller.infrastructure.adapter.in.rest.transaccion.dto.VentaResumenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ventas")
@RequiredArgsConstructor
@Tag(name = "Transacciones", description = "Gestión del ciclo de vida de ventas y su historial")
public class TransaccionController {

    private final CambiarEstadoVentaUseCase cambiarEstadoVentaUseCase;
    private final ConsultarHistorialVentaUseCase consultarHistorialVentaUseCase;
    private final ListarTransaccionesUseCase listarTransaccionesUseCase;
    private final TransaccionRestMapper mapper;

    @Operation(summary = "Cambiar estado de una venta")
    @PatchMapping("/{id}/estado")
    public Mono<ResponseEntity<VentaResumenResponse>> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody CambiarEstadoVentaRequest request) {
        return cambiarEstadoVentaUseCase.ejecutar(id, request.nuevoEstado(), request.justificacion(), request.actorId())
                .map(mapper::toResumen)
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar historial de estados de una venta")
    @GetMapping("/{id}/historial")
    public Flux<HistorialEstadoVentaResponse> consultarHistorial(@PathVariable UUID id) {
        return consultarHistorialVentaUseCase.ejecutar(id).map(mapper::toHistorialResponse);
    }

    @Operation(summary = "Listar transacciones con filtros opcionales")
    @GetMapping
    public Flux<VentaResumenResponse> listar(
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) UUID eventoId) {
        return listarTransaccionesUseCase.ejecutar(new FiltroTransacciones(estado, fechaInicio, fechaFin, eventoId))
                .map(mapper::toResumen);
    }
}
