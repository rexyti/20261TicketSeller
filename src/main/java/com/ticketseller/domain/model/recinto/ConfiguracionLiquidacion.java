package com.ticketseller.domain.model.recinto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionLiquidacion {
    private UUID recintoId;
    private ModeloNegocio modeloNegocio;
    private CategoriaRecinto tipoRecinto;
    private BigDecimal montoFijo;
}
