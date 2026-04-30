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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AplicarDescuentoCarritoUseCaseTest {

    @Mock
    private PromocionRepositoryPort promocionRepositoryPort;

    @Mock
    private DescuentoRepositoryPort descuentoRepositoryPort;

    @InjectMocks
    private AplicarDescuentoCarritoUseCase useCase;

    private UUID eventoId;
    private UUID zonaId;
    private BigDecimal subtotal;

    @BeforeEach
    void setUp() {
        eventoId = UUID.randomUUID();
        zonaId = UUID.randomUUID();
        subtotal = BigDecimal.valueOf(100);
    }

    @Test
    void debeLanzarErrorSiUsuarioNoAutorizadoParaPreventa() {
        Promocion preventaVIP = Promocion.builder()
                .tipoUsuarioRestringido(TipoUsuario.VIP)
                .build();

        when(promocionRepositoryPort.buscarActivasPorEvento(eq(eventoId), eq(TipoPromocion.PREVENTA), any(LocalDateTime.class)))
                .thenReturn(Flux.just(preventaVIP));
        when(descuentoRepositoryPort.buscarActivosPorEvento(eq(eventoId), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, zonaId, subtotal, TipoUsuario.GENERAL))
                .expectError(UsuarioNoAutorizadoParaPreventaException.class)
                .verify();
    }

    @Test
    void debeRetornarSinDescuentoSiNoHayDescuentosActivos() {
        when(promocionRepositoryPort.buscarActivasPorEvento(eq(eventoId), eq(TipoPromocion.PREVENTA), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        when(descuentoRepositoryPort.buscarActivosPorEvento(eq(eventoId), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());

        StepVerifier.create(useCase.ejecutar(eventoId, zonaId, subtotal, TipoUsuario.GENERAL))
                .expectNextMatches(resultado ->
                        resultado.montoDescuento().compareTo(BigDecimal.ZERO) == 0 &&
                        resultado.totalFinal().compareTo(subtotal) == 0 &&
                        "Sin descuento".equals(resultado.descripcion()))
                .verifyComplete();
    }

    @Test
    void debeAplicarMejorDescuentoNoAcumulable() {
        Descuento d1 = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.PORCENTAJE).valor(BigDecimal.valueOf(10)).acumulable(false).build();
        Descuento d2 = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.PORCENTAJE).valor(BigDecimal.valueOf(20)).acumulable(false).build();

        when(promocionRepositoryPort.buscarActivasPorEvento(eq(eventoId), eq(TipoPromocion.PREVENTA), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        when(descuentoRepositoryPort.buscarActivosPorEvento(eq(eventoId), any(LocalDateTime.class)))
                .thenReturn(Flux.just(d1, d2));

        StepVerifier.create(useCase.ejecutar(eventoId, zonaId, subtotal, TipoUsuario.GENERAL))
                .expectNextMatches(resultado ->
                        resultado.montoDescuento().compareTo(BigDecimal.valueOf(20)) == 0 &&
                        resultado.totalFinal().compareTo(BigDecimal.valueOf(80)) == 0 &&
                        resultado.descuentoId().equals(d2.getId()))
                .verifyComplete();
    }

    @Test
    void debeAplicarDescuentosAcumulablesYMejorNoAcumulable() {
        Descuento dAcumulable = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.MONTO_FIJO).valor(BigDecimal.valueOf(5)).acumulable(true).build();
        Descuento dNoAcumulable = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.PORCENTAJE).valor(BigDecimal.valueOf(10)).acumulable(false).build();

        when(promocionRepositoryPort.buscarActivasPorEvento(eq(eventoId), eq(TipoPromocion.PREVENTA), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        when(descuentoRepositoryPort.buscarActivosPorEvento(eq(eventoId), any(LocalDateTime.class)))
                .thenReturn(Flux.just(dAcumulable, dNoAcumulable));

        // Monto fijo = 5. Porcentaje = 10% de 100 = 10. Total descuento = 15.
        StepVerifier.create(useCase.ejecutar(eventoId, zonaId, subtotal, TipoUsuario.GENERAL))
                .expectNextMatches(resultado ->
                        resultado.montoDescuento().compareTo(BigDecimal.valueOf(15)) == 0 &&
                        resultado.totalFinal().compareTo(BigDecimal.valueOf(85)) == 0)
                .verifyComplete();
    }

    @Test
    void debeFiltrarDescuentoPorZona() {
        Descuento dZona = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.PORCENTAJE).valor(BigDecimal.valueOf(15)).zonaId(zonaId).acumulable(false).build();
        Descuento dOtraZona = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.PORCENTAJE).valor(BigDecimal.valueOf(50)).zonaId(UUID.randomUUID()).acumulable(false).build();

        when(promocionRepositoryPort.buscarActivasPorEvento(eq(eventoId), eq(TipoPromocion.PREVENTA), any(LocalDateTime.class)))
                .thenReturn(Flux.empty());
        when(descuentoRepositoryPort.buscarActivosPorEvento(eq(eventoId), any(LocalDateTime.class)))
                .thenReturn(Flux.just(dZona, dOtraZona));

        StepVerifier.create(useCase.ejecutar(eventoId, zonaId, subtotal, TipoUsuario.GENERAL))
                .expectNextMatches(resultado ->
                        resultado.montoDescuento().compareTo(BigDecimal.valueOf(15)) == 0 &&
                        resultado.descuentoId().equals(dZona.getId()))
                .verifyComplete();
    }

    @Test
    void aplicarConCodigoDebeCalcularMontoCorrectamente() {
        Descuento descuento = Descuento.builder().id(UUID.randomUUID()).tipo(TipoDescuento.PORCENTAJE).valor(BigDecimal.valueOf(50)).build();
        Ticket t1 = Ticket.builder().id(UUID.randomUUID()).precio(BigDecimal.valueOf(50)).build();
        Ticket t2 = Ticket.builder().id(UUID.randomUUID()).precio(BigDecimal.valueOf(50)).build();

        AplicacionDescuentoResultado resultado = useCase.aplicarConCodigo(subtotal, List.of(t1, t2), descuento, "HALFPRICE");

        assertEquals(0, resultado.montoDescuento().compareTo(BigDecimal.valueOf(50))); // 50% de 100
        assertEquals(0, resultado.totalFinal().compareTo(BigDecimal.valueOf(50)));
        assertEquals("Codigo HALFPRICE aplicado", resultado.descripcion());
    }
}
