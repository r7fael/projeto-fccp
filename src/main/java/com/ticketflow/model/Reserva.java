package com.ticketflow.model;

import java.time.Instant;

public class Reserva {

    private final String id;
    private final int assentoId;
    private final String usuario;
    private StatusReserva status;
    private Instant expiraEm;

    public Reserva(String id, int assentoId, String usuario, Instant expiraEm) {
        this.id = id;
        this.assentoId = assentoId;
        this.usuario = usuario;
        this.status = StatusReserva.PENDENTE;
        this.expiraEm = expiraEm;
    }

    public String getId() {
        return id;
    }

    public int getAssentoId() {
        return assentoId;
    }

    public String getUsuario() {
        return usuario;
    }

    public StatusReserva getStatus() {
        return status;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }

    public void marcarConfirmando() {
        this.status = StatusReserva.CONFIRMANDO;
    }

    public void marcarConfirmada() {
        this.status = StatusReserva.CONFIRMADA;
        this.expiraEm = null;
    }

    public void marcarCancelada() {
        this.status = StatusReserva.CANCELADA;
        this.expiraEm = null;
    }

    public void marcarExpirada() {
        this.status = StatusReserva.EXPIRADA;
        this.expiraEm = null;
    }
}
