package com.ticketseller.application.recinto;

import com.ticketseller.domain.model.recinto.CategoriaRecinto;
import com.ticketseller.domain.model.recinto.Recinto;
import com.ticketseller.domain.shared.Pagina;
import com.ticketseller.domain.repository.RecintoRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ListarRecintosFiltradosUseCase {

    private final RecintoRepositoryPort recintoRepositoryPort;

    public Mono<Pagina<Recinto>> ejecutar(String nombre,
                                          CategoriaRecinto categoria,
                                          String ciudad,
                                          Boolean activo,
                                          int page,
                                          int size,
                                          String sort) {
        return recintoRepositoryPort.listarFiltrados(nombre, categoria, ciudad, activo, page, size, sort);
    }
}

