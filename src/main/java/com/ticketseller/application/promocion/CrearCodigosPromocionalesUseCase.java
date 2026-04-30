package com.ticketseller.application.promocion;

import com.ticketseller.domain.exception.promocion.PromocionNoActivaException;
import com.ticketseller.domain.model.promocion.CodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoCodigoPromocional;
import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.repository.CodigoPromocionalRepositoryPort;
import com.ticketseller.domain.repository.PromocionRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class CrearCodigosPromocionalesUseCase {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int LONGITUD_SUFFIX = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final CodigoPromocionalRepositoryPort codigoPromocionalRepositoryPort;
    private final PromocionRepositoryPort promocionRepositoryPort;

    public Flux<CodigoPromocional> ejecutar(CrearCodigosPromocionalesCommand command) {
        return promocionRepositoryPort.buscarPorId(command.promocionId())
                .switchIfEmpty(reactor.core.publisher.Mono.error(new IllegalArgumentException("Promocion no encontrada")))
                .filter(promocion -> EstadoPromocion.ACTIVA.equals(promocion.getEstado()))
                .switchIfEmpty(reactor.core.publisher.Mono.error(
                        new PromocionNoActivaException("La promocion debe estar activa para generar codigos")))
                .flatMapMany(promocion -> {
                    validarCommand(command, promocion.getFechaInicio());
                    List<CodigoPromocional> codigos = construirCodigos(command, promocion.getFechaInicio());
                    return codigoPromocionalRepositoryPort.guardarTodos(codigos);
                });
    }

    private void validarCommand(CrearCodigosPromocionalesCommand command, LocalDateTime fechaInicioPromocion) {
        if (command.cantidad() < 1) {
            throw new IllegalArgumentException("cantidad debe ser mayor a 0");
        }
        if (command.fechaFin() == null || command.fechaFin().isBefore(fechaInicioPromocion)) {
            throw new IllegalArgumentException("fechaFin debe ser posterior o igual a fechaInicio de la promocion");
        }
        if (command.usosMaximosPorCodigo() != null && command.usosMaximosPorCodigo() < 1) {
            throw new IllegalArgumentException("usosMaximosPorCodigo debe ser mayor a 0");
        }
    }

    private List<CodigoPromocional> construirCodigos(CrearCodigosPromocionalesCommand command, LocalDateTime fechaInicio) {
        String prefijo = normalizarPrefijo(command.prefijo());
        Set<String> unicos = generarCodigosUnicos(command.cantidad(), prefijo);
        List<CodigoPromocional> codigos = new ArrayList<>(command.cantidad());
        for (String valorCodigo : unicos) {
            CodigoPromocional codigo = CodigoPromocional.builder()
                    .id(UUID.randomUUID())
                    .codigo(valorCodigo)
                    .promocionId(command.promocionId())
                    .usosMaximos(command.usosMaximosPorCodigo())
                    .usosActuales(0)
                    .fechaInicio(fechaInicio)
                    .fechaFin(command.fechaFin())
                    .estado(EstadoCodigoPromocional.ACTIVO)
                    .build()
                    .normalizarDatosRegistro();
            codigo.validarDatosRegistro();
            codigos.add(codigo);
        }
        return codigos;
    }

    private Set<String> generarCodigosUnicos(int cantidad, String prefijo) {
        Set<String> unicos = new LinkedHashSet<>(cantidad);
        int intentos = 0;
        int maxIntentos = cantidad * 20;
        while (unicos.size() < cantidad && intentos < maxIntentos) {
            unicos.add(prefijo + randomSuffix());
            intentos++;
        }
        if (unicos.size() < cantidad) {
            throw new IllegalStateException("No fue posible generar codigos unicos suficientes");
        }
        return unicos;
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder(LONGITUD_SUFFIX);
        for (int i = 0; i < LONGITUD_SUFFIX; i++) {
            sb.append(ALFABETO.charAt(RANDOM.nextInt(ALFABETO.length())));
        }
        return sb.toString();
    }

    private String normalizarPrefijo(String prefijo) {
        if (prefijo == null || prefijo.isBlank()) {
            return "PROMO-";
        }
        return prefijo.trim().toUpperCase(Locale.ROOT) + "-";
    }
}

