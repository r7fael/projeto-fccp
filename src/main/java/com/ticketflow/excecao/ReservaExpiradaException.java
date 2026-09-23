package com.ticketflow.excecao;

public class ReservaExpiradaException extends RuntimeException {
    public ReservaExpiradaException(String reservaId) {
        super("Reserva " + reservaId + " expirou.");
    }
}
