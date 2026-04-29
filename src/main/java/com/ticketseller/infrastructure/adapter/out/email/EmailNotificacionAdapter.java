package com.ticketseller.infrastructure.adapter.out.email;

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
    public Mono<Void> enviarConfirmacion(Venta venta, List<Ticket> tickets) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("comprador+" + venta.getCompradorId() + "@ticketseller.local");
            message.setSubject("Confirmación de compra " + venta.getId());
            message.setText("Tu compra fue completada. Tickets generados: " + tickets.size());
            javaMailSender.send(message);
        });
    }

    @Override
    public Mono<Void> enviarCancelacionTicket(Ticket ticket, String motivo) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("ticket+" + ticket.getVentaId() + "@ticketseller.local");
            message.setSubject("Cancelación de ticket " + ticket.getId());
            message.setText("Tu ticket fue cancelado. Motivo: " + motivo);
            javaMailSender.send(message);
        });
    }

    @Override
    public Mono<Void> enviarReembolsoCompletado(Venta venta, Ticket ticket, BigDecimal monto, String plazoAcreditacion) {
        return Mono.fromRunnable(() -> {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("comprador+" + venta.getCompradorId() + "@ticketseller.local");
            message.setSubject("Reembolso completado " + ticket.getId());
            message.setText("Tu reembolso por " + monto + " fue procesado. Acreditación estimada: " + plazoAcreditacion);
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
}

