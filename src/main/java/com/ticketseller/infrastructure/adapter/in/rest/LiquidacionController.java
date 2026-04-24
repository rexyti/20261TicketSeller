package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.application.liquidacion.ConfigurarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarRecaudoIncrementalUseCase;
import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.CondicionTicketResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.ConfigurarModeloNegocioRequest;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.ModeloNegocioResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.RecaudoIncrementalResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.liquidacion.SnapshotLiquidacionResponse;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LiquidacionController {

    private final ConsultarSnapshotUseCase consultarSnapshotUseCase;
    private final ConsultarModeloNegocioUseCase consultarModeloNegocioUseCase;
    private final ConfigurarModeloNegocioUseCase configurarModeloNegocioUseCase;
    private final ConsultarRecaudoIncrementalUseCase consultarRecaudoIncrementalUseCase;

    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modelo de negocio consultado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Recinto no encontrado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "El recinto no tiene modelo de negocio configurado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/recintos/{id}/modelo-negocio")
    public Mono<ResponseEntity<ModeloNegocioResponse>> consultarModeloNegocio(@PathVariable UUID id) {
        return consultarModeloNegocioUseCase.ejecutar(id)
                .map(config -> new ModeloNegocioResponse(
                        config.getRecintoId(),
                        config.getModeloNegocio(),
                        config.getTipoRecinto(),
                        config.getMontoFijo()))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Configurar el modelo de negocio de un recinto", description = "Permite a un administrador configurar el modelo de negocio (TARIFA_PLANA o REPARTO_INGRESOS) de un recinto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modelo de negocio configurado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Recinto no encontrado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PatchMapping("/recintos/{id}/modelo-negocio")
    public Mono<ResponseEntity<ModeloNegocioResponse>> configurarModeloNegocio(
            @PathVariable UUID id,
            @Valid @RequestBody ConfigurarModeloNegocioRequest request) {
        return configurarModeloNegocioUseCase.ejecutar(id, request.modelo(), request.montoFijo())
                .map(recinto -> new ModeloNegocioResponse(
                        recinto.getId(),
                        recinto.getModeloNegocio(),
                        recinto.getCategoria(),
                        recinto.getMontoFijo()))
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar el snapshot de cierre de un evento", description = "Obtiene el consolidado de estados de tickets de un evento cerrado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Snapshot consultado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "El evento no ha sido finalizado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/eventos/{id}/snapshot")
    public Mono<ResponseEntity<SnapshotLiquidacionResponse>> consultarSnapshot(@PathVariable UUID id) {
        return consultarSnapshotUseCase.ejecutar(id)
                .map(snapshot -> {
                    var condiciones = snapshot.getCondiciones().values().stream()
                            .map(c -> new CondicionTicketResponse(c.getCondicion(), c.getCantidad(), c.getValorTotal()))
                            .toList();
                    return new SnapshotLiquidacionResponse(
                            snapshot.getEventoId(),
                            condiciones,
                            snapshot.getTimestampGeneracion());
                })
                .map(ResponseEntity::ok);
    }

    @Operation(summary = "Consultar el recaudo incremental de un evento", description = "Obtiene el recaudo parcial acumulado durante un evento en curso.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Recaudo consultado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Evento no encontrado", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/eventos/{id}/recaudo")
    public Mono<ResponseEntity<RecaudoIncrementalResponse>> consultarRecaudo(@PathVariable UUID id) {
        return consultarRecaudoIncrementalUseCase.ejecutar(id)
                .map(recaudo -> new RecaudoIncrementalResponse(
                        id,
                        recaudo.get("recaudoRegular"),
                        recaudo.get("recaudoCortesia"),
                        recaudo.get("cancelaciones"),
                        recaudo.get("recaudoNeto"),
                        LocalDateTime.now()))
                .map(ResponseEntity::ok);
    }
}
