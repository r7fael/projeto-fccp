package com.ticketflow.repository;

import com.ticketflow.model.Reserva;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class ReservaRepository {

    private final Map<String, Reserva> reservas = new ConcurrentHashMap<>();

    public void salvar(Reserva reserva) {
        reservas.put(reserva.getId(), reserva);
    }

    public Reserva buscarPorId(String reservaId) {
        return reservas.get(reservaId);
    }

    public Collection<Reserva> listarTodas() {
        return reservas.values();
    }
}
