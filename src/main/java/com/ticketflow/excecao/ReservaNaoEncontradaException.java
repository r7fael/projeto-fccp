package com.ticketflow.excecao;

public class ReservaNaoEncontradaException extends RuntimeException {
    public ReservaNaoEncontradaException(String reservaId) {
        super("Reserva " + reservaId + " não existe.");
    }
}
