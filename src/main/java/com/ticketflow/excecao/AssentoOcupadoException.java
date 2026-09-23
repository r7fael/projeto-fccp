package com.ticketflow.excecao;

public class AssentoOcupadoException extends RuntimeException {
    public AssentoOcupadoException(int assentoId) {
        super("Assento " + assentoId + " já está reservado.");
    }
}
