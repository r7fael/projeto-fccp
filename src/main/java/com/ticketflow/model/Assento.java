package com.ticketflow.model;

import java.time.Instant;

public class Assento {

    private final int id;
    private StatusAssento status = StatusAssento.LIVRE;
    private String usuario;
    private Instant expiraEm;

    public Assento(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public StatusAssento getStatus() {
        return status;
    }

    public String getUsuario() {
        return usuario;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public void reservar(String usuario, Instant expiraEm) {
        this.status = StatusAssento.RESERVADO;
        this.usuario = usuario;
        this.expiraEm = expiraEm;
    }

    public void confirmar() {
        this.status = StatusAssento.VENDIDO;
        this.expiraEm = null;
    }

    public void liberar() {
        this.status = StatusAssento.LIVRE;
        this.usuario = null;
        this.expiraEm = null;
    }
}
