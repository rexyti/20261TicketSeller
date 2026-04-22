package com.ticketseller.infrastructure.adapter.in.rest;

import com.ticketseller.domain.exception.CapacidadInvalidaException;
import com.ticketseller.domain.exception.CompuertaInvalidaException;
import com.ticketseller.domain.exception.NombreTipoAsientoVacioException;
import com.ticketseller.domain.exception.RecintoConEventosException;
import com.ticketseller.domain.exception.RecintoDuplicadoException;
import com.ticketseller.domain.exception.RecintoInvalidoException;
import com.ticketseller.domain.exception.RecintoNotFoundException;
import com.ticketseller.domain.exception.TipoAsientoEnUsoException;
import com.ticketseller.domain.exception.TipoAsientoInactivoException;
import com.ticketseller.domain.exception.TipoAsientoNotFoundException;
import com.ticketseller.domain.exception.ZonaCapacidadExcedidaException;
import com.ticketseller.domain.exception.ZonaConTicketsVendidosException;
import com.ticketseller.domain.exception.ZonaInvalidaException;
import com.ticketseller.domain.exception.ZonaNotFoundException;
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

    @ExceptionHandler({RecintoConEventosException.class, RecintoDuplicadoException.class,
            TipoAsientoEnUsoException.class, TipoAsientoInactivoException.class})
    public ResponseEntity<ApiErrorResponse> conflict(RuntimeException ex) {
        return error("CONFLICT", ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({CapacidadInvalidaException.class, RecintoInvalidoException.class,
            ZonaInvalidaException.class, CompuertaInvalidaException.class, ZonaCapacidadExcedidaException.class,
            ZonaConTicketsVendidosException.class, NombreTipoAsientoVacioException.class,
            IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiErrorResponse> badRequest(RuntimeException ex) {
        return error("VALIDATION_ERROR", ex.getMessage(), HttpStatus.BAD_REQUEST);
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
