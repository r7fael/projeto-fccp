package com.ticketflow.service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

final class TravaPorAssento implements TravaAssento {

    private final ConcurrentHashMap<Integer, ReentrantLock> travas = new ConcurrentHashMap<>();

    @Override
    public void travar(int assentoId) {
        travas.computeIfAbsent(assentoId, id -> new ReentrantLock()).lock();
    }

    @Override
    public void destravar(int assentoId) {
        ReentrantLock trava = travas.get(assentoId);
        if (trava != null) {
            trava.unlock();
        }
    }
}
