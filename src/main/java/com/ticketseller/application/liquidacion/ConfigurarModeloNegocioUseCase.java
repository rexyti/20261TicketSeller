package com.ticketseller.application.liquidacion;

import com.ticketseller.domain.exception.recinto.RecintoNotFoundException;
import com.ticketseller.domain.model.recinto.ModeloNegocio;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class ConfigurarModeloNegocioUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Recinto> ejecutar(UUID recintoId, ModeloNegocio modelo, BigDecimal montoFijo) {
        return recintoRepositoryPort.buscarPorId(recintoId)
                .switchIfEmpty(Mono.error(new RecintoNotFoundException("Recinto no encontrado con id: " + recintoId)))
                .flatMap(recinto -> {
                    validarModelo(modelo, montoFijo, recinto);
                    Recinto actualizado = buildRecintoActualizado(recinto, modelo, montoFijo);
                    return recintoRepositoryPort.guardar(actualizado);
                });
    }

    private Recinto buildRecintoActualizado(Recinto recinto, ModeloNegocio modelo, BigDecimal montoFijo) {
        return recinto.toBuilder()
                .modeloNegocio(modelo)
                .montoFijo(ModeloNegocio.TARIFA_PLANA.equals(modelo) ? montoFijo : null)
                .build();
    }

    private void validarModelo(ModeloNegocio modelo, BigDecimal montoFijo, Recinto recinto) {
        if (noHayModelo(modelo)) {
            throw new IllegalArgumentException("El modelo de negocio es obligatorio");
        }
        if (tarifaPlanaConMontoInvalido(modelo, montoFijo)) {
            throw new IllegalArgumentException("El monto fijo es obligatorio y debe ser mayor a cero para TARIFA_PLANA");
        }
        if (recintoConTarifaRepartoYSinCategoria(recinto, modelo)) {
            throw new IllegalArgumentException("El recinto debe tener una categoría configurada para REPARTO_INGRESOS");
        }
    }

    private boolean noHayModelo(ModeloNegocio modelo) {
        return modelo == null;
    }

    private boolean tarifaPlanaConMontoInvalido(ModeloNegocio modelo, BigDecimal montoFijo) {
        return ModeloNegocio.TARIFA_PLANA.equals(modelo) && (montoFijo == null || montoFijo.compareTo(BigDecimal.ZERO) <= 0);
    }

    private boolean recintoConTarifaRepartoYSinCategoria(Recinto recinto, ModeloNegocio modelo){
        return ModeloNegocio.REPARTO_INGRESOS.equals(modelo) && recinto.getCategoria() == null;
    }
}
