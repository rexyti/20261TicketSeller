package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.domain.exception.CapacidadInvalidaException;
import com.ticketseller.domain.exception.CompuertaInvalidaException;
import com.ticketseller.domain.exception.NombreTipoAsientoVacioException;
import com.ticketseller.domain.exception.AsientoNoDisponibleException;
import com.ticketseller.domain.exception.AsientoReservadoPorOtroException;
import com.ticketseller.domain.exception.EventoEnProgresoException;
import com.ticketseller.domain.exception.EventoNoFinalizadoException;
import com.ticketseller.domain.exception.EventoNotFoundException;
import com.ticketseller.domain.exception.EventoSolapamientoException;
import com.ticketseller.domain.exception.LiquidacionNoConfiguradaException;
import com.ticketseller.domain.exception.PagoRechazadoException;
import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.exception.RecintoNoDisponibleException;
import com.ticketseller.domain.exception.RecintoDuplicadoException;
import com.ticketseller.domain.exception.RecintoInvalidoException;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.TipoAsientoEnUsoException;
import com.ticketseller.domain.exception.TipoAsientoInactivoException;
import com.ticketseller.domain.exception.TipoAsientoNotFoundException;
import com.ticketseller.domain.exception.ReservaExpiradaException;
import com.ticketseller.domain.exception.VentaNotFoundException;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.exception.ZonaConTicketsVendidosException;
import com.ticketseller.domain.exception.ZonaInvalidaException;
import com.ticketseller.domain.exception.ZonaNotFoundException;
import com.ticketseller.domain.exception.ZonaSinPrecioException;
import com.ticketseller.domain.exception.TransicionEstadoInvalidaException;
import com.ticketseller.domain.exception.AsientoEnCompraException;
import com.ticketseller.domain.exception.HoldExpiradoException;
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

    @ExceptionHandler({
            AsientoNoDisponibleException.class,
            ReservaExpiradaException.class,
            AsientoReservadoPorOtroException.class,
            HoldExpiradoException.class
    })
    public ResponseEntity<ApiErrorResponse> checkoutConflict(RuntimeException ex) {
        return error("CHECKOUT_CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
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
