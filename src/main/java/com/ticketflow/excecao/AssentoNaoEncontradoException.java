package com.ticketflow.excecao;

public class AssentoNaoEncontradoException extends RuntimeException {
    public AssentoNaoEncontradoException(int assentoId) {
        super("Assento " + assentoId + " não existe.");
    }
}
