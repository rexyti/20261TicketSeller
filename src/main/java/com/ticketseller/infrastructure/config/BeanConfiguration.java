package com.ticketseller.infrastructure.config;

import com.ticketseller.application.capacidad.ConfigurarCapacidadUseCase;
import com.ticketseller.application.capacidad.ConfigurarCategoriaUseCase;
import com.ticketseller.application.checkout.ConsultarVentaUseCase;
import com.ticketseller.application.checkout.LiberarReservaUseCase;
import com.ticketseller.application.checkout.ProcesarPagoUseCase;
import com.ticketseller.application.checkout.ReservarAsientosUseCase;
import com.ticketseller.application.compuerta.AsignarCompuertaAZonaUseCase;
import com.ticketseller.application.compuerta.CrearCompuertaUseCase;
import com.ticketseller.application.compuerta.ListarCompuertasUseCase;
import com.ticketseller.application.evento.*;
import com.ticketseller.application.liquidacion.ConfigurarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarModeloNegocioUseCase;
import com.ticketseller.application.liquidacion.ConsultarRecaudoIncrementalUseCase;
import com.ticketseller.application.liquidacion.ConsultarSnapshotUseCase;
import com.ticketseller.application.precios.ConfigurarPreciosUseCase;
import com.ticketseller.application.precios.ListarPreciosUseCase;
import com.ticketseller.application.recinto.DesactivarRecintoUseCase;
import com.ticketseller.application.recinto.EditarRecintoUseCase;
import com.ticketseller.application.recinto.ListarRecintosFiltradosUseCase;
import com.ticketseller.application.recinto.ListarRecintosUseCase;
import com.ticketseller.application.recinto.RegistrarRecintoUseCase;
import com.ticketseller.application.tipoasiento.AsignarTipoAsientoAZonaUseCase;
import com.ticketseller.application.asiento.CrearMapaAsientosUseCase;
import com.ticketseller.application.tipoasiento.CrearTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.DesactivarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.EditarTipoAsientoUseCase;
import com.ticketseller.application.tipoasiento.ListarTiposAsientoUseCase;
import com.ticketseller.application.asiento.MarcarEspacioVacioUseCase;
import com.ticketseller.application.zona.CrearZonaUseCase;
import com.ticketseller.application.zona.ListarZonasUseCase;
import com.ticketseller.application.zona.ValidarZonasUseCase;
import com.ticketseller.application.asiento.CambiarEstadoAsientoUseCase;
import com.ticketseller.application.asiento.CambiarEstadoMasivoUseCase;
import com.ticketseller.application.asiento.ConsultarHistorialAsientoUseCase;
import com.ticketseller.application.checkout.ConsultarEstadoTicketUseCase;
import com.ticketseller.application.recinto.ConsultarEstructuraRecintoUseCase;
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
import com.ticketseller.domain.repository.CancelacionEventoRepositoryPort;
import com.ticketseller.domain.repository.EventoRepositoryPort;
import com.ticketseller.domain.repository.LiquidacionQueryPort;
import com.ticketseller.domain.repository.PrecioZonaRepositoryPort;
import com.ticketseller.domain.repository.CodigoQrPort;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import com.ticketseller.domain.repository.PasarelaPagoPort;
import com.ticketseller.domain.repository.TicketRepositoryPort;
import com.ticketseller.domain.repository.TransaccionFinancieraRepositoryPort;
import com.ticketseller.domain.repository.VentaRepositoryPort;
import com.ticketseller.domain.repository.HistorialCambioEstadoRepositoryPort;
import com.ticketseller.infrastructure.adapter.out.payment.WompiAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.CancelacionEventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.CancelacionEventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.cancelacionevento.mapper.CancelacionEventoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.EventoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.evento.mapper.EventoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TicketR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TicketRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TransaccionFinancieraR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.TransaccionFinancieraRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.VentaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.VentaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.TicketPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.TransaccionFinancieraPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.checkout.mapper.VentaPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.liquidacion.LiquidacionQueryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.PrecioZonaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.PrecioZonaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.preciozona.mapper.PrecioZonaPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper.RecintoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.TipoAsientoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.TipoAsientoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.tipoasiento.mapper.TipoAsientoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.ZonaR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.ZonaRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.zona.mapper.ZonaPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.persistence.historialestadoasiento.HistorialCambioEstadoR2dbcRepository;
import com.ticketseller.infrastructure.adapter.out.persistence.historialestadoasiento.HistorialCambioEstadoRepositoryAdapter;
import com.ticketseller.infrastructure.adapter.out.persistence.historialestadoasiento.mapper.HistorialCambioEstadoPersistenceMapper;
import com.ticketseller.infrastructure.adapter.out.payment.PasarelaPagoAdapter;
import com.ticketseller.infrastructure.adapter.out.email.EmailNotificacionAdapter;
import com.ticketseller.infrastructure.adapter.out.qr.ZxingCodigoQrAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.mail.javamail.JavaMailSender;

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
    public TicketRepositoryPort ticketRepositoryPort(TicketR2dbcRepository repository,
                                                     TicketPersistenceMapper mapper) {
        return new TicketRepositoryAdapter(repository, mapper);
    }

    @Bean
    public VentaRepositoryPort ventaRepositoryPort(VentaR2dbcRepository repository,
                                                   VentaPersistenceMapper mapper) {
        return new VentaRepositoryAdapter(repository, mapper);
    }

    @Bean
    public TransaccionFinancieraRepositoryPort transaccionFinancieraRepositoryPort(
            TransaccionFinancieraR2dbcRepository repository,
            TransaccionFinancieraPersistenceMapper mapper) {
        return new TransaccionFinancieraRepositoryAdapter(repository, mapper);
    }

    @Bean
    public PasarelaPagoPort pasarelaPagoPort() {
        return new PasarelaPagoAdapter();
    }

    @Bean
    public NotificacionEmailPort notificacionEmailPort() {
        return new EmailNotificacionAdapter(javaMailSender());
    }

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    @Bean
    public CodigoQrPort codigoQrPort() {
        return new ZxingCodigoQrAdapter();
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
    public DesactivarTipoAsientoUseCase desactivarTipoAsientoUseCase(
            TipoAsientoRepositoryPort tipoAsientoRepositoryPort) {
        return new DesactivarTipoAsientoUseCase(tipoAsientoRepositoryPort);
    }

    @Bean
    public AsignarTipoAsientoAZonaUseCase asignarTipoAsientoAZonaUseCase(
            TipoAsientoRepositoryPort tipoAsientoRepositoryPort,
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

    @Bean
    public ReservarAsientosUseCase reservarAsientosUseCase(TicketRepositoryPort ticketRepositoryPort,
                                                           VentaRepositoryPort ventaRepositoryPort,
                                                           ZonaRepositoryPort zonaRepositoryPort,
                                                           PrecioZonaRepositoryPort precioZonaRepositoryPort,
                                                           CompuertaRepositoryPort compuertaRepositoryPort) {
        return new ReservarAsientosUseCase(ticketRepositoryPort, ventaRepositoryPort, zonaRepositoryPort,
                precioZonaRepositoryPort, compuertaRepositoryPort);
    }

    @Bean
    public LiberarReservaUseCase liberarReservaUseCase(VentaRepositoryPort ventaRepositoryPort,
                                                       TicketRepositoryPort ticketRepositoryPort) {
        return new LiberarReservaUseCase(ventaRepositoryPort, ticketRepositoryPort);
    }

    @Bean
    public ProcesarPagoUseCase procesarPagoUseCase(VentaRepositoryPort ventaRepositoryPort,
                                                   TicketRepositoryPort ticketRepositoryPort,
                                                   TransaccionFinancieraRepositoryPort transaccionFinancieraRepositoryPort,
                                                   PasarelaPagoPort pasarelaPagoPort,
                                                   NotificacionEmailPort notificacionEmailPort,
                                                   CodigoQrPort codigoQrPort) {
        return new ProcesarPagoUseCase(ventaRepositoryPort, ticketRepositoryPort,
                transaccionFinancieraRepositoryPort, pasarelaPagoPort, notificacionEmailPort, codigoQrPort);
    }

    @Bean
    public ConsultarVentaUseCase consultarVentaUseCase(VentaRepositoryPort ventaRepositoryPort,
                                                       TicketRepositoryPort ticketRepositoryPort) {
        return new ConsultarVentaUseCase(ventaRepositoryPort, ticketRepositoryPort);
    }

    @Bean
    public PasarelaPagoPort wompiAdapter(
            @Value("${wompi.base-url}") String baseUrl,
            @Value("${wompi.private-key}") String privateKey) {
        return new WompiAdapter(baseUrl, privateKey);
    }

    @Bean
    public LiquidacionQueryPort liquidacionQueryPort(DatabaseClient databaseClient) {
        return new LiquidacionQueryAdapter(databaseClient);
    }

    @Bean
    public ConsultarSnapshotUseCase consultarSnapshotUseCase(EventoRepositoryPort eventoRepositoryPort,
                                                             LiquidacionQueryPort liquidacionQueryPort) {
        return new ConsultarSnapshotUseCase(eventoRepositoryPort, liquidacionQueryPort);
    }

    @Bean
    public ConsultarModeloNegocioUseCase consultarModeloNegocioUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new ConsultarModeloNegocioUseCase(recintoRepositoryPort);
    }

    @Bean
    public ConfigurarModeloNegocioUseCase configurarModeloNegocioUseCase(RecintoRepositoryPort recintoRepositoryPort) {
        return new ConfigurarModeloNegocioUseCase(recintoRepositoryPort);
    }

    @Bean
    public ConsultarRecaudoIncrementalUseCase consultarRecaudoIncrementalUseCase(
            EventoRepositoryPort eventoRepositoryPort,
            LiquidacionQueryPort liquidacionQueryPort) {
        return new ConsultarRecaudoIncrementalUseCase(eventoRepositoryPort, liquidacionQueryPort);
    }

    @Bean
    public HistorialCambioEstadoRepositoryPort historialCambioEstadoRepositoryPort(
            HistorialCambioEstadoR2dbcRepository repository,
            HistorialCambioEstadoPersistenceMapper mapper) {
        return new HistorialCambioEstadoRepositoryAdapter(repository, mapper);
    }

    @Bean
    public CambiarEstadoAsientoUseCase cambiarEstadoAsientoUseCase(AsientoRepositoryPort asientoRepositoryPort, HistorialCambioEstadoRepositoryPort historialRepositoryPort) {
        return new CambiarEstadoAsientoUseCase(asientoRepositoryPort, historialRepositoryPort);
    }

    @Bean
    public CambiarEstadoMasivoUseCase cambiarEstadoMasivoUseCase(AsientoRepositoryPort asientoRepositoryPort, HistorialCambioEstadoRepositoryPort historialRepositoryPort) {
        return new CambiarEstadoMasivoUseCase(asientoRepositoryPort, historialRepositoryPort);
    }

    @Bean
    public ConsultarHistorialAsientoUseCase consultarHistorialAsientoUseCase(HistorialCambioEstadoRepositoryPort historialRepositoryPort) {
        return new ConsultarHistorialAsientoUseCase(historialRepositoryPort);
    }

    @Bean
    public ConsultarEstadoTicketUseCase consultarEstadoTicketUseCase(TicketRepositoryPort ticketRepositoryPort) {
        return new ConsultarEstadoTicketUseCase(ticketRepositoryPort);
    }

    @Bean
    public ConsultarEstructuraRecintoUseCase consultarEstructuraRecintoUseCase(RecintoRepositoryPort recintoRepositoryPort,
                                                                               ZonaRepositoryPort zonaRepositoryPort) {
        return new ConsultarEstructuraRecintoUseCase(recintoRepositoryPort, zonaRepositoryPort);
    }
}
