package com.ticketflow.worker;

import com.ticketflow.service.ReservaService;

import java.util.concurrent.BlockingQueue;

public class ConfirmadorPagamento implements Runnable {

    public static final String PILULA_ENVENENADA = "__PARAR__";

    private final BlockingQueue<String> fila;
    private final ReservaService reservaService;

    public ConfirmadorPagamento(BlockingQueue<String> fila, ReservaService reservaService) {
        this.fila = fila;
        this.reservaService = reservaService;
    }

    @Override
    public void run() {
        while (true) {
            String reservaId;
            try {
                reservaId = fila.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            if (PILULA_ENVENENADA.equals(reservaId)) {
                return;
            }
            try {
                simularProcessamentoPagamento();
                reservaService.completarConfirmacao(reservaId);
            } catch (RuntimeException e) {
                System.err.println("Erro ao confirmar reserva " + reservaId + ": " + e.getMessage());
            }
        }
    }

    private void simularProcessamentoPagamento() {
        try {
            Thread.sleep(100 + (long) (Math.random() * 200));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
