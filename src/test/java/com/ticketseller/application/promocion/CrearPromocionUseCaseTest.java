package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.evento.EventoNotFoundException;
import com.ticketseller.domain.model.evento.Evento;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CrearPromocionUseCaseTest {

    private PromocionRepositoryPort promocionRepositoryPort;
    private EventoRepositoryPort eventoRepositoryPort;
    private CrearPromocionUseCase useCase;

    @BeforeEach
    void setUp() {
        promocionRepositoryPort = mock(PromocionRepositoryPort.class);
        eventoRepositoryPort = mock(EventoRepositoryPort.class);
        useCase = new CrearPromocionUseCase(promocionRepositoryPort, eventoRepositoryPort);
    }

    @Test
    void deberiaCrearPromocionConEstadoActiva() {
        UUID eventoId = UUID.randomUUID();
        Promocion request = buildRequest(eventoId, TipoPromocion.PREVENTA, TipoUsuario.VIP);

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.just(Evento.builder().id(eventoId).build()));
        when(promocionRepositoryPort.guardar(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(useCase.ejecutar(request))
                .assertNext(p -> {
                    assertThat(p.getEstado()).isEqualTo(EstadoPromocion.ACTIVA);
                    assertThat(p.getId()).isNotNull();
                    assertThat(p.getTipoUsuarioRestringido()).isEqualTo(TipoUsuario.VIP);
                })
                .verifyComplete();
    }

    @Test
    void deberiaFallarSiEventoNoExiste() {
        UUID eventoId = UUID.randomUUID();
        Promocion request = buildRequest(eventoId, TipoPromocion.PREVENTA, null);

        when(eventoRepositoryPort.buscarPorId(eventoId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(EventoNotFoundException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiFechaInicioEsPosteriorAFechaFin() {
        UUID eventoId = UUID.randomUUID();
        Promocion request = Promocion.builder()
                .nombre("Promo")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now().plusDays(5))
                .fechaFin(LocalDateTime.now().plusDays(1))
                .build();

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void deberiaFallarSiFechaInicioEsIgualAFechaFin() {
        UUID eventoId = UUID.randomUUID();
        LocalDateTime mismo = LocalDateTime.now().plusDays(1);
        Promocion request = Promocion.builder()
                .nombre("Promo")
                .tipo(TipoPromocion.DESCUENTO)
                .eventoId(eventoId)
                .fechaInicio(mismo)
                .fechaFin(mismo)
                .build();

        StepVerifier.create(useCase.ejecutar(request))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    private Promocion buildRequest(UUID eventoId, TipoPromocion tipo, TipoUsuario tipoUsuario) {
        return Promocion.builder()
                .nombre("Preventa VIP")
                .tipo(tipo)
                .eventoId(eventoId)
                .fechaInicio(LocalDateTime.now())
                .fechaFin(LocalDateTime.now().plusDays(7))
                .tipoUsuarioRestringido(tipoUsuario)
                .build();
    }
}
