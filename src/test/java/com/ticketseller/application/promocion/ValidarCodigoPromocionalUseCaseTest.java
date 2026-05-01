package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.CodigoPromoAgotadoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoExpiradoException;
import com.ticketseller.domain.exception.promocion.CodigoPromoInvalidoException;
import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.Descuento;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoDescuento;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidarCodigoPromocionalUseCaseTest {

    private CodigoPromocionalRepositoryPort codigoRepositoryPort;
    private PromocionRepositoryPort promocionRepositoryPort;
    private DescuentoRepositoryPort descuentoRepositoryPort;
    private ValidarCodigoPromocionalUseCase useCase;

    @BeforeEach
    void setUp() {
        codigoRepositoryPort = mock(CodigoPromocionalRepositoryPort.class);
        promocionRepositoryPort = mock(PromocionRepositoryPort.class);
        descuentoRepositoryPort = mock(DescuentoRepositoryPort.class);
        useCase = new ValidarCodigoPromocionalUseCase(codigoRepositoryPort, promocionRepositoryPort, descuentoRepositoryPort);
    }

    @Test
    void deberiaRetornarDescuentoConCodigoValido() {
        UUID promocionId = UUID.randomUUID();
        UUID codigoId = UUID.randomUUID();
        CodigoPromocional codigo = buildCodigoValido(codigoId, promocionId, 10, 3);
        Promocion promocion = buildPromocionActiva(promocionId);
        Descuento descuento = buildDescuento(promocionId);

        when(codigoRepositoryPort.buscarPorCodigo("VERANO10")).thenReturn(Mono.just(codigo));
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(promocion));
        when(codigoRepositoryPort.incrementarUsos(codigoId)).thenReturn(Mono.just(codigo));
        when(descuentoRepositoryPort.buscarPorPromocionId(promocionId)).thenReturn(Flux.just(descuento));

        StepVerifier.create(useCase.ejecutar("VERANO10"))
                .assertNext(d -> assertThat(d.getPromocionId()).isEqualTo(promocionId))
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiCodigoNoExiste() {
        when(codigoRepositoryPort.buscarPorCodigo("INVALIDO")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar("INVALIDO"))
                .expectError(CodigoPromoInvalidoException.class)
                .verify();
    }

    @Test
    void deberiaFallarConMensajeCodigoExpirado() {
        UUID promocionId = UUID.randomUUID();
        CodigoPromocional expirado = buildCodigoExpirado(promocionId);

        when(codigoRepositoryPort.buscarPorCodigo("OLD10")).thenReturn(Mono.just(expirado));

        StepVerifier.create(useCase.ejecutar("OLD10"))
                .expectErrorMatches(e -> e instanceof CodigoPromoExpiradoException
                        && e.getMessage().equals("CÓDIGO EXPIRADO"))
                .verify();
    }

    @Test
    void deberiaFallarConMensajeCodigoYaUtilizado() {
        UUID promocionId = UUID.randomUUID();
        CodigoPromocional agotado = buildCodigoAgotado(promocionId);

        when(codigoRepositoryPort.buscarPorCodigo("USED")).thenReturn(Mono.just(agotado));

        StepVerifier.create(useCase.ejecutar("USED"))
                .expectErrorMatches(e -> e instanceof CodigoPromoAgotadoException
                        && e.getMessage().equals("CÓDIGO YA UTILIZADO"))
                .verify();
    }

    @Test
    void deberiaFallarSiPromocionNoEstaActiva() {
        UUID promocionId = UUID.randomUUID();
        UUID codigoId = UUID.randomUUID();
        CodigoPromocional codigo = buildCodigoValido(codigoId, promocionId, 5, 2);
        Promocion pausada = buildPromocionActiva(promocionId).toBuilder()
                .estado(EstadoPromocion.PAUSADA)
                .build();

        when(codigoRepositoryPort.buscarPorCodigo("PAUSED")).thenReturn(Mono.just(codigo));
        when(promocionRepositoryPort.buscarPorId(promocionId)).thenReturn(Mono.just(pausada));

        StepVerifier.create(useCase.ejecutar("PAUSED"))
                .expectError(PromocionNoActivaException.class)
                .verify();
    }

    private CodigoPromocional buildCodigoValido(UUID id, UUID promocionId, int usosMax, int usosActuales) {
        return CodigoPromocional.builder()
                .id(id)
                .codigo("VERANO10")
                .promocionId(promocionId)
                .usosMaximos(usosMax)
                .usosActuales(usosActuales)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(30))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();
    }

    private CodigoPromocional buildCodigoExpirado(UUID promocionId) {
        return CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("OLD10")
                .promocionId(promocionId)
                .usosMaximos(100)
                .usosActuales(0)
                .fechaInicio(LocalDateTime.now().minusDays(30))
                .fechaFin(LocalDateTime.now().minusDays(1))
                .estado(EstadoCodigoPromocional.EXPIRADO)
                .build();
    }

    private CodigoPromocional buildCodigoAgotado(UUID promocionId) {
        return CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo("USED")
                .promocionId(promocionId)
                .usosMaximos(1)
                .usosActuales(1)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(30))
                .estado(EstadoCodigoPromocional.AGOTADO)
                .build();
    }

    private Promocion buildPromocionActiva(UUID id) {
        return Promocion.builder()
                .id(id)
                .nombre("Campaña Verano")
                .tipo(TipoPromocion.CODIGOS)
                .eventoId(UUID.randomUUID())
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(30))
                .estado(EstadoPromocion.ACTIVA)
                .build();
    }

    private Descuento buildDescuento(UUID promocionId) {
        return Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(promocionId)
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(new BigDecimal("10"))
                .acumulable(false)
                .build();
    }
}
