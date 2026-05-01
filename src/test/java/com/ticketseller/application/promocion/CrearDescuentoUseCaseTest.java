package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.exception.promocion.PromocionNotFoundException;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrearDescuentoUseCaseTest {

    private DescuentoRepositoryPort descuentoRepositoryPort;
    private PromocionRepositoryPort promocionRepositoryPort;
    private ZonaRepositoryPort zonaRepositoryPort;
    private CrearDescuentoUseCase useCase;

    @BeforeEach
    void setUp() {
        descuentoRepositoryPort = mock(DescuentoRepositoryPort.class);
        promocionRepositoryPort = mock(PromocionRepositoryPort.class);
        zonaRepositoryPort = mock(ZonaRepositoryPort.class);
        useCase = new CrearDescuentoUseCase(descuentoRepositoryPort, promocionRepositoryPort, zonaRepositoryPort);
    }

    @Test
    void deberiaCrearDescuentoPorcentual() {
        UUID promocionId = UUID.randomUUID();
        Promocion promocion = buildPromocionActiva(promocionId);
        Descuento request = Descuento.builder()
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("20"))
                .acumulable(false)
                .build();

        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocion));
        when(descuentoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(promocionId, request))
                .assertNext(d -> {
                    assertThat(d.getId()).isNotNull();
                    assertThat(d.getPromocionId()).isEqualTo(promocionId);
                    assertThat(d.getValor()).isEqualByComparingTo("20");
                })
                .verifyComplete();
    }

    @Test
    void deberiaCrearDescuentoMontoFijo() {
        UUID promocionId = UUID.randomUUID();
        Promocion promocion = buildPromocionActiva(promocionId);
        Descuento request = Descuento.builder()
                .tipo(TipoDescuento.MONTO_FIJO)
                .valor(new BigDecimal("15000"))
                .acumulable(false)
                .build();

        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocion));
        when(descuentoRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(promocionId, request))
                .assertNext(d -> assertThat(d.getTipo()).isEqualTo(TipoDescuento.MONTO_FIJO))
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiPromocionNoExiste() {
        UUID promocionId = UUID.randomUUID();
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.empty());

        Descuento request = Descuento.builder()
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .build();

        StepVerifier.create(useCase.ejecutar(promocionId, request))
                .expectError(PromocionNotFoundException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiPromocionNoCestaActiva() {
        UUID promocionId = UUID.randomUUID();
        Promocion pausada = buildPromocionActiva(promocionId).toBuilder()
                .estado(EstadoPromocion.PAUSADA)
                .build();

        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(pausada));

        Descuento request = Descuento.builder()
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .build();

        StepVerifier.create(useCase.ejecutar(promocionId, request))
                .expectError(PromocionNoActivaException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiPorcentajeSuperaCien() {
        UUID promocionId = UUID.randomUUID();
        Promocion promocion = buildPromocionActiva(promocionId);
        Descuento request = Descuento.builder()
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("110"))
                .build();

        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocion));

        StepVerifier.create(useCase.ejecutar(promocionId, request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiValorEsCero() {
        UUID promocionId = UUID.randomUUID();
        Promocion promocion = buildPromocionActiva(promocionId);
        Descuento request = Descuento.builder()
                .tipo(TipoDescuento.MONTO_FIJO)
                .valor(BigDecimal.ZERO)
                .build();

        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocion));

        StepVerifier.create(useCase.ejecutar(promocionId, request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    private Promocion buildPromocionActiva(UUID id) {
        return Promocion.builder()
                .id(id)
                .nombre("Descuento Flash")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(7))
                .estado(EstadoPromocion.ACTIVA)
                .build();
    }
}
