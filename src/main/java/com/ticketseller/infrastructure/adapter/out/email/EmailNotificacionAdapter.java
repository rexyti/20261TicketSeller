package com.ticketseller.infrastructure.adapter.out.email;

import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.port.out.NotificacionEmailPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class EmailNotificacionAdapter implements NotificacionEmailPort {

    @Override
    public Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets) {
        log.info("Enviando email de confirmación para venta {} con {} tickets", venta.getId(), tickets.size());
        // TODO: implementar con JavaMailSender cuando se configure el servidor SMTP
        return Mono.empty();
    }
}
