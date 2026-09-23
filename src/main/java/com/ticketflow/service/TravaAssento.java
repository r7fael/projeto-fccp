package com.ticketflow.service;

public interface TravaAssento {

    void travar(int assentoId);

    void destravar(int assentoId);

    static TravaAssento paraModo(ModoSincronizacao modo) {
        return switch (modo) {
            case SEM_TRAVA -> new TravaSemProtecao();
            case TRAVA_GLOBAL -> new TravaGlobal();
            case TRAVA_POR_ASSENTO -> new TravaPorAssento();
        };
    }
}
