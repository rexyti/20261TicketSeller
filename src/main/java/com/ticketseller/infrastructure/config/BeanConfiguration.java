package com.ticketseller.infrastructure.config;

import com.ticketseller.application.capacidad.ConfigurarCapacidadUseCase;
import com.ticketseller.application.capacidad.ConfigurarCategoriaUseCase;
import com.ticketseller.application.compuerta.AsignarCompuertaAZonaUseCase;
import com.ticketseller.application.compuerta.CrearCompuertaUseCase;
import com.ticketseller.application.compuerta.ListarCompuertasUseCase;
import com.ticketseller.application.recinto.DesactivarRecintoUseCase;
import com.ticketseller.application.recinto.EditarRecintoUseCase;
import com.ticketseller.application.recinto.ListarRecintosFiltradosUseCase;
import com.ticketseller.application.recinto.ListarRecintosUseCase;
import com.ticketseller.application.recinto.RegistrarRecintoUseCase;
import com.ticketseller.application.tipoasiento.AsignarTipoAsientoAZonaUseCase;
import com.ticketseller.application.tipoasiento.CrearMapaAsientosUseCase;
import com.ticketseller.application.tipoasiento.CrearTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.DesactivarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.EditarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.ListarTiposAsientoUseCase;
import com.ticketseller.application.tipoasiento.MarcarEspacioVacioUseCase;
import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.application.zona.ValidarZonasUseCase;
import com.ticketseller.domain.repository.AsientoRepositoryPort;
import com.ticketseller.domain.repository.CompuertaRepositoryPort;
import com.ticketseller.domain.repository.MapaAsientosRepositoryPort;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import com.ticketseller.domain.repository.TipoAsientoRepositoryPort;
import com.ticketseller.domain.repository.ZonaRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.AsientoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.AsientoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.asiento.mapper.AsientoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.CompuertaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.CompuertaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.compuerta.mapper.CompuertaPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.mapaasientos.MapaAsientosRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper.RecintoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.TipoAsientoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.TipoAsientoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.mapper.TipoAsientoPersistenceMapper;
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
    public TipoAsientoRepositoryPort tipoAsientoRepositoryPort(TipoAsientoR2dbcRepository repository,
                                                               TipoAsientoPersistenceMapper mapper) {
        return new TipoAsientoRepositoryAdapter(repository, mapper);
    }

    @Bean
    public AsientoRepositoryPort asientoRepositoryPort(AsientoR2dbcRepository repository,
                                                       AsientoPersistenceMapper mapper) {
        return new AsientoRepositoryAdapter(repository, mapper);
    }

    @Bean
    public MapaAsientosRepositoryPort mapaAsientosRepositoryPort(DatabaseClient databaseClient) {
        return new MapaAsientosRepositoryAdapter(databaseClient);
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
    public CrearTipoAsientoUseCase crearTipoAsientoUseCase(TipoAsientoRepositoryPort tipoAsientoRepositoryPort) {
        return new CrearTipoAsientoUseCase(tipoAsientoRepositoryPort);
    }

    @Bean
    public EditarTipoAsientoUseCase editarTipoAsientoUseCase(TipoAsientoRepositoryPort tipoAsientoRepositoryPort) {
        return new EditarTipoAsientoUseCase(tipoAsientoRepositoryPort);
    }

    @Bean
    public ListarTiposAsientoUseCase listarTiposAsientoUseCase(TipoAsientoRepositoryPort tipoAsientoRepositoryPort) {
        return new ListarTiposAsientoUseCase(tipoAsientoRepositoryPort);
    }

    @Bean
    public DesactivarTipoAsientoUseCase desactivarTipoAsientoUseCase(TipoAsientoRepositoryPort tipoAsientoRepositoryPort) {
        return new DesactivarTipoAsientoUseCase(tipoAsientoRepositoryPort);
    }

    @Bean
    public AsignarTipoAsientoAZonaUseCase asignarTipoAsientoAZonaUseCase(TipoAsientoRepositoryPort tipoAsientoRepositoryPort,
                                                                          ZonaRepositoryPort zonaRepositoryPort) {
        return new AsignarTipoAsientoAZonaUseCase(tipoAsientoRepositoryPort, zonaRepositoryPort);
    }

    @Bean
    public CrearMapaAsientosUseCase crearMapaAsientosUseCase(AsientoRepositoryPort asientoRepositoryPort,
                                                             MapaAsientosRepositoryPort mapaAsientosRepositoryPort) {
        return new CrearMapaAsientosUseCase(asientoRepositoryPort, mapaAsientosRepositoryPort);
    }

    @Bean
    public MarcarEspacioVacioUseCase marcarEspacioVacioUseCase(AsientoRepositoryPort asientoRepositoryPort) {
        return new MarcarEspacioVacioUseCase(asientoRepositoryPort);
    }
}
