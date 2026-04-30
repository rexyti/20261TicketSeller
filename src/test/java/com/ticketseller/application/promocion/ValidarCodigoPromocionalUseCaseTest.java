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
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.DescuentoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidarCodigoPromocionalUseCaseTest {

    @Mock
    private CodigoPromocionalRepositoryPort codigoPromocionalRepositoryPort;

    @Mock
    private PromocionRepositoryPort promocionRepositoryPort;

    @Mock
    private DescuentoRepositoryPort descuentoRepositoryPort;

    @InjectMocks
    private ValidarCodigoPromocionalUseCase useCase;

    private CodigoPromocional codigoValido;
    private Promocion promocionActiva;
    private Descuento descuento;
    private String textoCodigo = "TEST-123";

    @BeforeEach
    void setUp() {
        codigoValido = CodigoPromocional.builder()
                .id(UUID.randomUUID())
                .codigo(textoCodigo)
                .promocionId(UUID.randomUUID())
                .usosMaximos(5)
                .usosActuales(1)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaFin(LocalDateTime.now().plusDays(5))
                .estado(EstadoCodigoPromocional.ACTIVO)
                .build();

        promocionActiva = Promocion.builder()
                .id(codigoValido.getPromocionId())
                .estado(EstadoPromocion.ACTIVA)
                .build();

        descuento = Descuento.builder()
                .id(UUID.randomUUID())
                .promocionId(promocionActiva.getId())
                .tipo(TipoDescuento.PORCENTAJE)
                .valor(BigDecimal.valueOf(20))
                .build();
    }

    @Test
    void debeValidarCodigoExitosamenteYRetornarDescuento() {
        when(codigoPromocionalRepositoryPort.buscarPorCodigo(textoCodigo)).thenReturn(Mono.just(codigoValido));
        when(promocionRepositoryPort.buscarPorId(codigoValido.getPromocionId())).thenReturn(Mono.just(promocionActiva));
        when(codigoPromocionalRepositoryPort.incrementarUsoAtomico(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(true));
        when(descuentoRepositoryPort.buscarPorPromocionId(codigoValido.getPromocionId())).thenReturn(Flux.just(descuento));

        StepVerifier.create(useCase.ejecutar(textoCodigo))
                .expectNextMatches(d -> d.getId().equals(descuento.getId()))
                .verifyComplete();

        verify(codigoPromocionalRepositoryPort).incrementarUsoAtomico(anyString(), any(LocalDateTime.class));
    }

    @Test
    void debeLanzarErrorSiCodigoEsNuloOVacio() {
        StepVerifier.create(useCase.ejecutar(""))
                .expectError(CodigoPromoInvalidoException.class)
                .verify();

        verifyNoInteractions(codigoPromocionalRepositoryPort);
    }

    @Test
    void debeLanzarErrorSiCodigoNoExiste() {
        when(codigoPromocionalRepositoryPort.buscarPorCodigo("INEXISTENTE")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar("INEXISTENTE"))
                .expectError(CodigoPromoInvalidoException.class)
                .verify();
    }

    @Test
    void debeLanzarErrorSiCodigoExpirado() {
        CodigoPromocional expirado = CodigoPromocional.builder()
                .estado(EstadoCodigoPromocional.EXPIRADO)
                .fechaFin(LocalDateTime.now().minusDays(1))
                .build();

        when(codigoPromocionalRepositoryPort.buscarPorCodigo(textoCodigo)).thenReturn(Mono.just(expirado));

        StepVerifier.create(useCase.ejecutar(textoCodigo))
                .expectErrorMatches(t -> t instanceof CodigoPromoExpiradoException && t.getMessage().equals("CODIGO EXPIRADO"))
                .verify();
    }

    @Test
    void debeLanzarErrorSiCodigoAgotado() {
        CodigoPromocional agotado = CodigoPromocional.builder()
                .estado(EstadoCodigoPromocional.ACTIVO)
                .fechaFin(LocalDateTime.now().plusDays(1))
                .usosMaximos(1)
                .usosActuales(1) // agotado
                .build();

        when(codigoPromocionalRepositoryPort.buscarPorCodigo(textoCodigo)).thenReturn(Mono.just(agotado));

        StepVerifier.create(useCase.ejecutar(textoCodigo))
                .expectErrorMatches(t -> t instanceof CodigoPromoAgotadoException && t.getMessage().equals("CODIGO YA UTILIZADO"))
                .verify();
    }

    @Test
    void debeLanzarErrorSiPromocionNoEstaActiva() {
        Promocion pausada = Promocion.builder().estado(EstadoPromocion.PAUSADA).build();

        when(codigoPromocionalRepositoryPort.buscarPorCodigo(textoCodigo)).thenReturn(Mono.just(codigoValido));
        when(promocionRepositoryPort.buscarPorId(codigoValido.getPromocionId())).thenReturn(Mono.just(pausada));

        StepVerifier.create(useCase.ejecutar(textoCodigo))
                .expectError(PromocionNoActivaException.class)
                .verify();
    }

    @Test
    void debeLanzarErrorSiIncrementoAtomicoFalla() {
        when(codigoPromocionalRepositoryPort.buscarPorCodigo(textoCodigo)).thenReturn(Mono.just(codigoValido));
        when(promocionRepositoryPort.buscarPorId(codigoValido.getPromocionId())).thenReturn(Mono.just(promocionActiva));
        // Devuelve false porque el código pudo haber sido usado por otra transacción simultáneamente
        when(codigoPromocionalRepositoryPort.incrementarUsoAtomico(anyString(), any(LocalDateTime.class))).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.ejecutar(textoCodigo))
                .expectErrorMatches(t -> t instanceof CodigoPromoAgotadoException && t.getMessage().equals("CODIGO YA UTILIZADO"))
                .verify();
    }
}
