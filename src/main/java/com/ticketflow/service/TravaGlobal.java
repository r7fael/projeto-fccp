package com.ticketflow.service;

import java.util.concurrent.locks.ReentrantLock;

final class TravaGlobal implements TravaAssento {

    private final ReentrantLock trava = new ReentrantLock();

    @Override
    public void travar(int assentoId) {
        trava.lock();
    }

    @Override
    public void destravar(int assentoId) {
        trava.unlock();
    }
}
