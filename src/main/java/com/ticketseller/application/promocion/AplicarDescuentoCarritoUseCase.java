package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.UsuarioNoAutorizadoParaPreventaException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class AplicarDescuentoCarritoUseCase {

    private final PromocionRepositoryPort promocionRepositoryPort;
    private final DescuentoRepositoryPort descuentoRepositoryPort;

    public Mono<AplicacionDescuentoResultado> ejecutar(UUID eventoId,
                                                       UUID zonaId,
                                                       BigDecimal subtotal,
                                                       TipoUsuario tipoUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        return validarAccesoPreventa(eventoId, tipoUsuario == null ? TipoUsuario.GENERAL : tipoUsuario, ahora)
                .then(descuentoRepositoryPort.buscarActivosPorEvento(eventoId, ahora).collectList())
                .map(descuentos -> aplicarAutomaticos(subtotal, zonaId, descuentos));
    }

    public AplicacionDescuentoResultado aplicarConCodigo(BigDecimal subtotal,
                                                         List<Ticket> tickets,
                                                         Descuento descuento,
                                                         String codigo) {
        BigDecimal montoDescuento = calcularMontoDescuentoPorTickets(tickets, descuento);
        BigDecimal totalFinal = subtotal.subtract(montoDescuento).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        return new AplicacionDescuentoResultado(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                montoDescuento.setScale(2, RoundingMode.HALF_UP),
                totalFinal,
                descuento.getId(),
                "Codigo %s aplicado".formatted(codigo)
        );
    }

    private Mono<Void> validarAccesoPreventa(UUID eventoId, TipoUsuario tipoUsuario, LocalDateTime ahora) {
        return promocionRepositoryPort.buscarActivasPorEvento(eventoId, TipoPromocion.PREVENTA, ahora)
                .collectList()
                .flatMap(preventas -> {
                    boolean autorizado = preventas.isEmpty() || preventas.stream().allMatch(promocion ->
                            promocion.getTipoUsuarioRestringido() == null
                                    || promocion.getTipoUsuarioRestringido().equals(tipoUsuario));
                    if (!autorizado) {
                        return Mono.error(new UsuarioNoAutorizadoParaPreventaException(
                                "El usuario no esta autorizado para la preventa activa"
                        ));
                    }
                    return Mono.empty();
                });
    }

    private AplicacionDescuentoResultado aplicarAutomaticos(BigDecimal subtotal, UUID zonaId, List<Descuento> descuentos) {
        if (subtotal == null || subtotal.compareTo(BigDecimal.ZERO) <= 0 || descuentos.isEmpty()) {
            return sinDescuento(subtotal);
        }

        List<Descuento> aplicables = descuentos.stream()
                .filter(descuento -> descuento.getZonaId() == null || descuento.getZonaId().equals(zonaId))
                .toList();
        if (aplicables.isEmpty()) {
            return sinDescuento(subtotal);
        }

        List<Descuento> acumulables = aplicables.stream().filter(Descuento::isAcumulable).toList();
        Descuento mejorNoAcumulable = aplicables.stream()
                .filter(descuento -> !descuento.isAcumulable())
                .max(Comparator.comparing(descuento -> calcularMontoDescuento(subtotal, descuento)))
                .orElse(null);

        BigDecimal montoTotal = Flux.fromIterable(acumulables)
                .map(descuento -> calcularMontoDescuento(subtotal, descuento))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .blockOptional()
                .orElse(BigDecimal.ZERO);
        if (mejorNoAcumulable != null) {
            montoTotal = montoTotal.add(calcularMontoDescuento(subtotal, mejorNoAcumulable));
        }
        montoTotal = montoTotal.min(subtotal);
        BigDecimal totalFinal = subtotal.subtract(montoTotal).setScale(2, RoundingMode.HALF_UP);
        return new AplicacionDescuentoResultado(
                subtotal.setScale(2, RoundingMode.HALF_UP),
                montoTotal.setScale(2, RoundingMode.HALF_UP),
                totalFinal,
                mejorNoAcumulable == null ? null : mejorNoAcumulable.getId(),
                montoTotal.compareTo(BigDecimal.ZERO) > 0 ? "Descuento automatico aplicado" : "Sin descuento"
        );
    }

    private BigDecimal calcularMontoDescuentoPorTickets(List<Ticket> tickets, Descuento descuento) {
        BigDecimal base = tickets.stream()
                .filter(ticket -> descuento.getZonaId() == null || descuento.getZonaId().equals(ticket.getZonaId()))
                .map(Ticket::getPrecio)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return calcularMontoDescuento(base, descuento);
    }

    private BigDecimal calcularMontoDescuento(BigDecimal base, Descuento descuento) {
        if (TipoDescuento.PORCENTAJE.equals(descuento.getTipo())) {
            return base.multiply(descuento.getValor())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .min(base);
        }
        return descuento.getValor().min(base);
    }

    private AplicacionDescuentoResultado sinDescuento(BigDecimal subtotal) {
        BigDecimal subtotalSeguro = subtotal == null ? BigDecimal.ZERO : subtotal.setScale(2, RoundingMode.HALF_UP);
        return new AplicacionDescuentoResultado(
                subtotalSeguro,
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                subtotalSeguro,
                null,
                "Sin descuento"
        );
    }
}

