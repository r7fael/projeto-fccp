package com.ticketflow.repository;

import com.ticketflow.excecao.AssentoNaoEncontradoException;
import com.ticketflow.model.Assento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class AssentoRepository {

    private final Map<Integer, Assento> assentos = new ConcurrentHashMap<>();

    public AssentoRepository(@Value("${ticketflow.quantidade-assentos}") int quantidadeAssentos) {
        for (int id = 1; id <= quantidadeAssentos; id++) {
            assentos.put(id, new Assento(id));
        }
    }

    public Assento buscarPorId(int assentoId) {
        Assento assento = assentos.get(assentoId);
        if (assento == null) {
            throw new AssentoNaoEncontradoException(assentoId);
        }
        return assento;
    }

    public List<Assento> listarTodos() {
        List<Assento> resultado = new ArrayList<>(assentos.values());
        resultado.sort(Comparator.comparingInt(Assento::getId));
        return resultado;
    }
}
