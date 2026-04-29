package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.domain.exception.CapacidadInvalidaException;
import com.ticketseller.domain.exception.CompuertaInvalidaException;
import com.ticketseller.domain.exception.asiento.NombreTipoAsientoVacioException;
import com.ticketseller.domain.exception.asiento.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.postventa.CancelacionFueraDePlazoException;
import com.ticketseller.domain.exception.postventa.ReembolsoFallidoException;
import com.ticketseller.domain.exception.postventa.TicketYaUsadoException;
import com.ticketseller.domain.exception.evento.EventoEnProgresoException;
import com.ticketseller.domain.exception.evento.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.exception.evento.EventoSolapamientoException;
import com.ticketseller.domain.exception.LiquidacionNoConfiguradaException;
import com.ticketseller.domain.exception.venta.PagoRechazadoException;
import com.ticketseller.domain.exception.venta.TicketNotFoundException;
import com.ticketseller.domain.exception.recinto.RecintoConEventosException;
import com.ticketseller.domain.exception.recinto.RecintoNoDisponibleException;
import com.ticketseller.domain.exception.recinto.RecintoDuplicadoException;
import com.ticketseller.domain.exception.recinto.RecintoInvalidoException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.exception.asiento.TipoAsientoEnUsoException;
import com.ticketseller.domain.exception.asiento.TipoAsientoInactivoException;
import com.ticketseller.domain.exception.asiento.TipoAsientoNotFoundException;
import com.ticketseller.domain.exception.venta.ReservaExpiradaException;
import com.ticketseller.domain.exception.venta.VentaNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.exception.zona.ZonaConTicketsVendidosException;
import com.ticketseller.domain.exception.zona.ZonaInvalidaException;
import com.ticketseller.domain.exception.zona.ZonaNotFoundException;
import com.ticketseller.domain.exception.zona.ZonaSinPrecioException;
import com.ticketseller.domain.exception.asiento.TransicionEstadoInvalidaException;
import com.ticketseller.domain.exception.asiento.AsientoEnCompraException;
import com.ticketseller.infrastructure.adapter.in.rest.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({RecintoNotFoundException.class, TipoAsientoNotFoundException.class, ZonaNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> notFound(RuntimeException ex) {
        return error("NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> ticketNotFound(TicketNotFoundException ex) {
        return error("TICKET_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EventoNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> eventoNotFound(EventoNotFoundException ex) {
        return error("EVENTO_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(VentaNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> ventaNotFound(VentaNotFoundException ex) {
        return error("VENTA_NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({RecintoConEventosException.class, RecintoDuplicadoException.class,
            RecintoNoDisponibleException.class, EventoEnProgresoException.class, EventoSolapamientoException.class,
            EventoNoFinalizadoException.class,
            TipoAsientoEnUsoException.class, TipoAsientoInactivoException.class,
            TransicionEstadoInvalidaException.class, AsientoEnCompraException.class})
    public ResponseEntity<ApiErrorResponse> conflict(RuntimeException ex) {
        return error("CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({AsientoNoDisponibleException.class, ReservaExpiradaException.class})
    public ResponseEntity<ApiErrorResponse> checkoutConflict(RuntimeException ex) {
        return error("CHECKOUT_CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({TicketYaUsadoException.class})
    public ResponseEntity<ApiErrorResponse> ticketConflict(RuntimeException ex) {
        return error("TICKET_CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PagoRechazadoException.class)
    public ResponseEntity<ApiErrorResponse> paymentRejected(PagoRechazadoException ex) {
        return error("PAGO_RECHAZADO", ex.getMessage(), HttpStatus.PAYMENT_REQUIRED);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> gatewayUnavailable(IllegalStateException ex) {
        return error("PASARELA_NO_DISPONIBLE", ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler({CapacidadInvalidaException.class, RecintoInvalidoException.class,
            ZonaInvalidaException.class, CompuertaInvalidaException.class, ZonaCapacidadExcedidaException.class,
            ZonaConTicketsVendidosException.class, NombreTipoAsientoVacioException.class,
            IllegalArgumentException.class, //IllegalStateException.class,
            ZonaConTicketsVendidosException.class, ZonaSinPrecioException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException ex) {
        return error("VALIDATION_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LiquidacionNoConfiguradaException.class)
    public ResponseEntity<ApiErrorResponse> liquidacionNoConfigurada(LiquidacionNoConfiguradaException ex) {
        return error("LIQUIDACION_NO_CONFIGURADA", ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler({CancelacionFueraDePlazoException.class,
            com.ticketseller.domain.exception.postventa.TransicionEstadoInvalidaException.class})
    public ResponseEntity<ApiErrorResponse> unprocessable(RuntimeException ex) {
        return error("UNPROCESSABLE_ENTITY", ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @ExceptionHandler(ReembolsoFallidoException.class)
    public ResponseEntity<ApiErrorResponse> refundFailed(ReembolsoFallidoException ex) {
        return error("REEMBOLSO_FALLIDO", ex.getMessage(), HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ApiErrorResponse> bindError(WebExchangeBindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return error("VALIDATION_ERROR", message, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiErrorResponse> error(String code, String message, HttpStatus status) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(code, message, LocalDateTime.now()));
    }
}
