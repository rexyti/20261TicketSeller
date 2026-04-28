package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.LiquidacionNoConfiguradaException;
import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import com.ticketseller.domain.model.recinto.ConfiguracionLiquidacion;
import com.ticketseller.domain.model.recinto.ModeloNegocio;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsultarModeloNegocioUseCaseTest {

    private final RecintoRepositoryPort recintoRepositoryPort = mock(RecintoRepositoryPort.class);
    private final ConsultarModeloNegocioUseCase useCase = new ConsultarModeloNegocioUseCase(recintoRepositoryPort);

    @Test
    void deberiaRetornarConfiguracionCuandoModeloConfigurado() {
        UUID recintoId = UUID.randomUUID();
        Recinto recinto = Recinto.builder().id(recintoId).modeloNegocio(ModeloNegocio.TARIFA_PLANA).build();
        ConfiguracionLiquidacion config = ConfiguracionLiquidacion.builder()
                .recintoId(recintoId)
                .modeloNegocio(ModeloNegocio.TARIFA_PLANA)
                .montoFijo(BigDecimal.valueOf(5000))
                .build();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(recintoRepositoryPort.buscarConfiguracionLiquidacion(recintoId)).thenReturn(Mono.just(config));

        StepVerifier.create(useCase.ejecutar(recintoId))
                .assertNext(result -> {
                    assert result.getModeloNegocio().equals(ModeloNegocio.TARIFA_PLANA);
                    assert result.getMontoFijo().compareTo(BigDecimal.valueOf(5000)) == 0;
                })
                .verifyComplete();
    }

    @Test
    void deberiaRetornarConfiguracionRepartoIngresos() {
        UUID recintoId = UUID.randomUUID();
        Recinto recinto = Recinto.builder().id(recintoId).modeloNegocio(ModeloNegocio.REPARTO_INGRESOS).build();
        ConfiguracionLiquidacion config = ConfiguracionLiquidacion.builder()
                .recintoId(recintoId)
                .modeloNegocio(ModeloNegocio.REPARTO_INGRESOS)
                .tipoRecinto(CategoriaRecinto.ESTADIO)
                .build();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(recintoRepositoryPort.buscarConfiguracionLiquidacion(recintoId)).thenReturn(Mono.just(config));

        StepVerifier.create(useCase.ejecutar(recintoId))
                .assertNext(result -> {
                    assert result.getModeloNegocio().equals(ModeloNegocio.REPARTO_INGRESOS);
                    assert result.getTipoRecinto().equals(CategoriaRecinto.ESTADIO);
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarCuandoRecintoSinModelo() {
        UUID recintoId = UUID.randomUUID();
        Recinto recinto = Recinto.builder().id(recintoId).build();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.just(recinto));
        when(recintoRepositoryPort.buscarConfiguracionLiquidacion(recintoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectError(LiquidacionNoConfiguradaException.class)
                .verify();
    }

    @Test
    void deberiaFallarCuandoRecintoNoExiste() {
        UUID recintoId = UUID.randomUUID();

        when(recintoRepositoryPort.buscarPorId(recintoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(recintoId))
                .expectError(RecintoNotFoundException.class)
                .verify();
    }
}
