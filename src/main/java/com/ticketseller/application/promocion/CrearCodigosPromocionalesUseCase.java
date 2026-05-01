package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class CrearCodigosPromocionalesUseCase {

    private final CodigoPromocionalRepositoryPort codigoRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;

    public Flux<CodigoPromocional> ejecutar(UUID promocionId, int cantidad, Integer usosMaximosPorCodigo,
                                            String prefijo, LocalDateTime fechaFin) {
        return promocionRepositoryPort.buscarPorId(promocionId)
                .switchIfEmpty(Mono.error(new PromocionNotFoundException("La promoción indicada no existe")))
                .filter(Promocion::estaActiva)
                .switchIfEmpty(Mono.error(new PromocionNoActivaException("Solo se pueden generar códigos para promociones activas")))
                .flatMapMany(p -> {
                    List<CodigoPromocional> codigos = generarCodigos(promocionId, cantidad, usosMaximosPorCodigo,
                            prefijo, LocalDateTime.now(), fechaFin);
                    return codigoRepositoryPort.guardarTodos(codigos);
                });
    }

    private CodigoPromocional buildCodigo(UUID promocionId, String base, Integer usosMaximos,
                                          LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        return CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo(base + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase())
                .promocionId(promocionId)
                .usosMaximos(usosMaximos)
                .usosActuales(0)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();
    }

    private List<CodigoPromocional> generarCodigos(UUID promocionId, int cantidad,
                                                   Integer usosMaximosPorCodigo, String prefijo,
                                                   LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        String base = prefijo != null && !prefijo.isBlank() ? prefijo.toUpperCase() + "-" : "";
        return IntStream.range(0, cantidad)
                .mapToObj(i -> buildCodigo(promocionId, base, usosMaximosPorCodigo, fechaInicio, fechaFin))
                .toList();
    }
}
