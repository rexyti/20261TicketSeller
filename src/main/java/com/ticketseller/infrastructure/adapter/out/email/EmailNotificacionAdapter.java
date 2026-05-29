package com.ticketseller.infrastructure.adapter.out.email;

import com.ticketseller.domain.model.bloqueos.Cortesia;
import com.ticketseller.domain.model.ticket.Ticket;
import com.ticketseller.domain.model.venta.Venta;
import com.ticketseller.domain.repository.NotificacionEmailPort;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class EmailNotificacionAdapter implements NotificacionEmailPort {

    private final JavaMailSender javaMailSender;

    @Override
    public Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets, String emailComprador) {
        var text = """
                Tu compra fue completada. Tickets generados: %d.
                Guarda bien tus qr para la entrada al evento.
                """.formatted(tickets.size());
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailComprador);
            message.setSubject("Confirmación de compra " + venta.getId());
            message.setText(text);
            javaMailSender.send(message);
        });
    }

    @Override
    public Mono<Void> enviarCancelacionTicket(Ticket ticket, String motivo, String emailComprador) {
        var text = """
                Tu ticket fue cancelado.
                Motivo: %s
                """.formatted(motivo);
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailComprador);
            message.setSubject("Cancelación de ticket " + ticket.getId());
            message.setText("Tu ticket fue cancelado. Motivo: " + motivo);
            javaMailSender.send(message);
        });
    }

    @Override
    public Mono<Void> enviarReembolsoCompletado(Venta venta, Ticket ticket, BigDecimal monto, String plazoAcreditacion, String emailComprador) {
        var text = """
                Tu reembolso por %f fue procesado.
                Acreditación estimada: %s
                """.formatted(monto, plazoAcreditacion);

        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailComprador);
            message.setSubject("Reembolso completado " + ticket.getId());
            message.setText(text);
            javaMailSender.send(message);
        });
    }

    @Override
    public Mono<Void> enviarAlertaSoporteReembolsoFallido(UUID reembolsoId, String detalle) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("soporte@ticketseller.local");
            message.setSubject("Reembolso fallido " + reembolsoId);
            message.setText(detalle);
            javaMailSender.send(message);
        });
    }

    @Override
    public Mono<Void> enviarCortesiaGenerada(Cortesia cortesia, String nombreEvento, String emailDestinatario) {
        var text = """
                Se te ha generado una cortesía (ID: %s) para el evento "%s".
                Revisa en tus tickets, descarga el qr, y presentate con el en la entrada.
                """.formatted(cortesia.getId(), nombreEvento);
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailDestinatario);
            message.setSubject("Has recibido una cortesía para: " + nombreEvento);
            message.setText(text);
            javaMailSender.send(message);
        });
    }
}

