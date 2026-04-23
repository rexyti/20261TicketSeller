package com.ticketseller.infrastructure.config;

import com.ticketseller.application.capacidad.ConfigurarCapacidadUseCase;
import com.ticketseller.application.capacidad.ConfigurarCategoriaUseCase;
import com.ticketseller.application.compuerta.AsignarCompuertaAZonaUseCase;
import com.ticketseller.application.compuerta.CrearCompuertaUseCase;
import com.ticketseller.application.compuerta.ListarCompuertasUseCase;
import com.ticketseller.application.evento.*;
import com.ticketseller.application.precios.ConfigurarPreciosUseCase;
import com.ticketseller.application.precios.ListarPreciosUseCase;
import com.ticketseller.application.recinto.DesactivarRecintoUseCase;
import com.ticketseller.application.recinto.EditarRecintoUseCase;
import com.ticketseller.application.recinto.ListarRecintosFiltradosUseCase;
import com.ticketseller.application.recinto.ListarRecintosUseCase;
import com.ticketseller.application.recinto.RegistrarRecintoUseCase;
import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.application.zona.ValidarZonasUseCase;
import com.ticketseller.domain.repository.CancelacionEventoRepositoryPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.CancelacionEventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.CancelacionEventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.mapper.CancelacionEventoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.CompuertaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.CompuertaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.mapper.CompuertaPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.PrecioZonaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.PrecioZonaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.mapper.PrecioZonaPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper.RecintoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.ZonaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.ZonaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.mapper.ZonaPersistenceMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;

@Configuration
public class BeanConfiguration {

    @Bean
    public RecintoRepositoryPort recintoRepositoryPort(RecintoR2dbcRepository repository,
                                                       RecintoPersistenceMapper mapper,
                                                       DatabaseClient databaseClient) {
        return new RecintoRepositoryAdapter(repository, mapper, databaseClient);
    }

    @Bean
    public ZonaRepositoryPort zonaRepositoryPort(ZonaR2dbcRepository repository,
                                                 ZonaPersistenceMapper mapper) {
        return new ZonaRepositoryAdapter(repository, mapper);
    }

    @Bean
    public CompuertaRepositoryPort compuertaRepositoryPort(CompuertaR2dbcRepository repository,
                                                           CompuertaPersistenceMapper mapper) {
        return new CompuertaRepositoryAdapter(repository, mapper);
    }

    @Bean
    public EventoRepositoryPort eventoRepositoryPort(EventoR2dbcRepository repository,
                                                     EventoPersistenceMapper mapper) {
        return new EventoRepositoryAdapter(repository, mapper);
    }

    @Bean
    public CancelacionEventoRepositoryPort cancelacionEventoRepositoryPort(CancelacionEventoR2dbcRepository repository,
                                                                           CancelacionEventoPersistenceMapper mapper) {
        return new CancelacionEventoRepositoryAdapter(repository, mapper);
    }

    @Bean
    public PrecioZonaRepositoryPort precioZonaRepositoryPort(PrecioZonaR2dbcRepository repository,
                                                             PrecioZonaPersistenceMapper mapper) {
        return new PrecioZonaRepositoryAdapter(repository, mapper);
    }

    @Bean
    public RegistrarRecintoUseCase registrarRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new RegistrarRecintoUseCase(recintoRepositoryPort);
    }

    @Bean
    public ListarRecintosUseCase listarRecintosUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new ListarRecintosUseCase(recintoRepositoryPort);
    }

    @Bean
    public ListarRecintosFiltradosUseCase listarRecintosFiltradosUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new ListarRecintosFiltradosUseCase(recintoRepositoryPort);
    }

    @Bean
    public ConfigurarCapacidadUseCase configurarCapacidadUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new ConfigurarCapacidadUseCase(recintoRepositoryPort);
    }

    @Bean
    public EditarRecintoUseCase editarRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new EditarRecintoUseCase(recintoRepositoryPort);
    }

    @Bean
    public DesactivarRecintoUseCase desactivarRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new DesactivarRecintoUseCase(recintoRepositoryPort);
    }

    @Bean
    public ConfigurarCategoriaUseCase configurarCategoriaUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new ConfigurarCategoriaUseCase(recintoRepositoryPort);
    }

    @Bean
    public CrearZonaUseCase crearZonaUseCase(ZonaRepositoryPort zonaRepositoryPort,
                                             RecintoRepositoryPort recintoRepositoryPort) {
        return new CrearZonaUseCase(zonaRepositoryPort, recintoRepositoryPort);
    }

    @Bean
    public ListarZonasUseCase listarZonasUseCase(ZonaRepositoryPort zonaRepositoryPort) {
        return new ListarZonasUseCase(zonaRepositoryPort);
    }

    @Bean
    public ValidarZonasUseCase validarZonasUseCase(ZonaRepositoryPort zonaRepositoryPort,
                                                   RecintoRepositoryPort recintoRepositoryPort) {
        return new ValidarZonasUseCase(zonaRepositoryPort, recintoRepositoryPort);
    }

    @Bean
    public CrearCompuertaUseCase crearCompuertaUseCase(CompuertaRepositoryPort compuertaRepositoryPort,
                                                       RecintoRepositoryPort recintoRepositoryPort,
                                                       ZonaRepositoryPort zonaRepositoryPort) {
        return new CrearCompuertaUseCase(compuertaRepositoryPort, recintoRepositoryPort, zonaRepositoryPort);
    }

    @Bean
    public AsignarCompuertaAZonaUseCase asignarCompuertaAZonaUseCase(CompuertaRepositoryPort compuertaRepositoryPort,
                                                                     ZonaRepositoryPort zonaRepositoryPort) {
        return new AsignarCompuertaAZonaUseCase(compuertaRepositoryPort, zonaRepositoryPort);
    }

    @Bean
    public ListarCompuertasUseCase listarCompuertasUseCase(CompuertaRepositoryPort compuertaRepositoryPort) {
        return new ListarCompuertasUseCase(compuertaRepositoryPort);
    }

    @Bean
    public RegistrarEventoUseCase registrarEventoUseCase(EventoRepositoryPort eventoRepositoryPort,
                                                         RecintoRepositoryPort recintoRepositoryPort) {
        return new RegistrarEventoUseCase(eventoRepositoryPort, recintoRepositoryPort);
    }

    @Bean
    public ListarEventosUseCase listarEventosUseCase(EventoRepositoryPort eventoRepositoryPort) {
        return new ListarEventosUseCase(eventoRepositoryPort);
    }

    @Bean
    public ConfigurarPreciosUseCase configurarPreciosUseCase(EventoRepositoryPort eventoRepositoryPort,
                                                             PrecioZonaRepositoryPort precioZonaRepositoryPort,
                                                             ZonaRepositoryPort zonaRepositoryPort) {
        return new ConfigurarPreciosUseCase(eventoRepositoryPort, precioZonaRepositoryPort, zonaRepositoryPort);
    }

    @Bean
    public ListarPreciosUseCase listarPreciosUseCase(EventoRepositoryPort eventoRepositoryPort,
                                                     PrecioZonaRepositoryPort precioZonaRepositoryPort) {
        return new ListarPreciosUseCase(eventoRepositoryPort, precioZonaRepositoryPort);
    }

    @Bean
    public EditarEventoUseCase editarEventoUseCase(EventoRepositoryPort eventoRepositoryPort) {
        return new EditarEventoUseCase(eventoRepositoryPort);
    }

    @Bean
    public CancelarEventoUseCase cancelarEventoUseCase(EventoRepositoryPort eventoRepositoryPort,
                                                       CancelacionEventoRepositoryPort cancelacionEventoRepositoryPort) {
        return new CancelarEventoUseCase(eventoRepositoryPort, cancelacionEventoRepositoryPort);
    }
}
