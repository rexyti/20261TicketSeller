package com.ticketseller.domain.model.recinto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionLiquidacion {
    private ModeloNegocio modeloNegocio;
    private BigDecimal montoFijo;
}
