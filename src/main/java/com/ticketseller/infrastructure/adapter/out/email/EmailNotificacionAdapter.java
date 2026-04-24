package com.ticketseller.infrastructure.adapter.out.email;

import com.ticketseller.domain.model.Ticket;
import com.ticketseller.domain.model.Venta;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
public class EmailNotificacionAdapter implements NotificacionEmailPort {

    private final JavaMailSender javaMailSender;

    @Override
    public Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("comprador+" + venta.getCompradorId() + "@ticketseller.local");
            message.setSubject("Confirmación de compra " + venta.getId());
            message.setText("Tu compra fue completada. Tickets generados: " + tickets.size());
            javaMailSender.send(message);
        });
    }
}

