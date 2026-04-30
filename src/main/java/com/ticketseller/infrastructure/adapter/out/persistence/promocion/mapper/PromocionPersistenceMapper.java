package com.ticketseller.infrastructure.adapter.out.persistence.promocion.mapper;

import com.ticketseller.domain.model.promocion.EstadoPromocion;
import com.ticketseller.domain.model.promocion.Promocion;
import com.ticketseller.domain.model.promocion.TipoPromocion;
import com.ticketseller.domain.model.promocion.TipoUsuario;
import com.ticketseller.infrastructure.adapter.out.persistence.promocion.PromocionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PromocionPersistenceMapper {

    @Mapping(target = "tipo", expression = "java(domain.getTipo() == null ? null : domain.getTipo().name())")
    @Mapping(target = "estado", expression = "java(domain.getEstado() == null ? null : domain.getEstado().name())")
    @Mapping(
            target = "tipoUsuarioRestringido",
            expression = "java(domain.getTipoUsuarioRestringido() == null ? null : domain.getTipoUsuarioRestringido().name())"
    )
    PromocionEntity toEntity(Promocion domain);

    @Mapping(target = "tipo", expression = "java(toTipo(entity.getTipo()))")
    @Mapping(target = "estado", expression = "java(toEstado(entity.getEstado()))")
    @Mapping(target = "tipoUsuarioRestringido", expression = "java(toTipoUsuario(entity.getTipoUsuarioRestringido()))")
    Promocion toDomain(PromocionEntity entity);

    default TipoPromocion toTipo(String tipo) {
        return tipo == null ? null : TipoPromocion.valueOf(tipo);
    }

    default EstadoPromocion toEstado(String estado) {
        return estado == null ? null : EstadoPromocion.valueOf(estado);
    }

    default TipoUsuario toTipoUsuario(String tipoUsuario) {
        return tipoUsuario == null ? null : TipoUsuario.valueOf(tipoUsuario);
    }
}

