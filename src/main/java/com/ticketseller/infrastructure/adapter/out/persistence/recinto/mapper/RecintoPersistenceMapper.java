package com.ticketseller.infrastructure.adapter.out.persistence.recinto.mapper;

import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import com.ticketseller.domain.model.recinto.ConfiguracionLiquidacion;
import com.ticketseller.domain.model.recinto.ModeloNegocio;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.infrastructure.adapter.out.persistence.recinto.RecintoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface RecintoPersistenceMapper {

    @Mapping(target = "categoria", expression = "java(toCategoriaString(recinto.getCategoria()))")
    @Mapping(target = "modeloNegocio", expression = "java(toModeloNegocioString(recinto.getConfiguracionLiquidacion() != null ? recinto.getConfiguracionLiquidacion().getModeloNegocio() : null))")
    @Mapping(target = "montoFijo", expression = "java(recinto.getConfiguracionLiquidacion() != null ? recinto.getConfiguracionLiquidacion().getMontoFijo() : null)")
    RecintoEntity toEntity(Recinto recinto);

    @Mapping(target = "categoria", expression = "java(toCategoriaEnum(entity.getCategoria()))")
    @Mapping(target = "configuracionLiquidacion", expression = "java(toConfiguracionLiquidacion(entity.getModeloNegocio(), entity.getMontoFijo()))")
    Recinto toDomain(RecintoEntity entity);

    default String toCategoriaString(CategoriaRecinto categoria) {
        return categoria == null ? null : categoria.name();
    }

    default CategoriaRecinto toCategoriaEnum(String categoria) {
        return categoria == null ? null : CategoriaRecinto.valueOf(categoria);
    }

    default String toModeloNegocioString(ModeloNegocio modeloNegocio) {
        return modeloNegocio == null ? null : modeloNegocio.name();
    }

    default ModeloNegocio toModeloNegocioEnum(String modeloNegocio) {
        return modeloNegocio == null ? null : ModeloNegocio.valueOf(modeloNegocio);
    }

    default ConfiguracionLiquidacion toConfiguracionLiquidacion(String modeloNegocio, BigDecimal montoFijo) {
        if (modeloNegocio == null) {
            return null;
        }
        return ConfiguracionLiquidacion.builder()
                .modeloNegocio(toModeloNegocioEnum(modeloNegocio))
                .montoFijo(montoFijo)
                .build();
    }
}
