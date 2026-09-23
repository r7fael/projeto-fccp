package com.ticketflow.service;

import com.ticketflow.excecao.AssentoOcupadoException;
import com.ticketflow.excecao.ReservaExpiradaException;
import com.ticketflow.excecao.ReservaNaoEncontradaException;
import com.ticketflow.model.Assento;
import com.ticketflow.model.Reserva;
import com.ticketflow.model.StatusAssento;
import com.ticketflow.model.StatusReserva;
import com.ticketflow.repository.AssentoRepository;
import com.ticketflow.repository.ReservaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ReservaService {

    private final AssentoRepository assentoRepository;
    private final ReservaRepository reservaRepository;
    private final BlockingQueue<String> filaConfirmacao;
    private final TravaAssento trava;
    private final ModoSincronizacao modo;
    private final Duration ttlReserva;
    private final AtomicInteger contadorReserva = new AtomicInteger(100);

    public ReservaService(AssentoRepository assentoRepository,
                           ReservaRepository reservaRepository,
                           BlockingQueue<String> filaConfirmacao,
                           @Value("${ticketflow.modo-sincronizacao}") ModoSincronizacao modo,
                           @Value("${ticketflow.ttl-reserva-segundos}") long ttlReservaSegundos) {
        this.assentoRepository = assentoRepository;
        this.reservaRepository = reservaRepository;
        this.filaConfirmacao = filaConfirmacao;
        this.modo = modo;
        this.trava = TravaAssento.paraModo(modo);
        this.ttlReserva = Duration.ofSeconds(ttlReservaSegundos);
    }

    public List<Assento> listarAssentos() {
        return assentoRepository.listarTodos();
    }

    public Reserva reservar(String usuario, int assentoId) {
        Assento assento = assentoRepository.buscarPorId(assentoId);
        trava.travar(assentoId);
        try {
            if (assento.getStatus() != StatusAssento.LIVRE) {
                throw new AssentoOcupadoException(assentoId);
            }
            if (modo == ModoSincronizacao.SEM_TRAVA) {
                dormirArtificialmente();
            }
            Instant expiraEm = Instant.now().plus(ttlReserva);
            assento.reservar(usuario, expiraEm);
            String id = "r-" + contadorReserva.incrementAndGet();
            Reserva reserva = new Reserva(id, assentoId, usuario, expiraEm);
            reservaRepository.salvar(reserva);
            return reserva;
        } finally {
            trava.destravar(assentoId);
        }
    }

    public Reserva confirmar(String reservaId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId);
        if (reserva == null) {
            throw new ReservaNaoEncontradaException(reservaId);
        }
        trava.travar(reserva.getAssentoId());
        try {
            if (reserva.getStatus() == StatusReserva.PENDENTE && estaExpirada(reserva)) {
                expirarComTravaAdquirida(reserva);
            }
            if (reserva.getStatus() != StatusReserva.PENDENTE) {
                throw new ReservaExpiradaException(reservaId);
            }
            reserva.marcarConfirmando();
            enfileirar(reserva.getId());
            return reserva;
        } finally {
            trava.destravar(reserva.getAssentoId());
        }
    }

    public void completarConfirmacao(String reservaId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId);
        if (reserva == null) {
            return;
        }
        trava.travar(reserva.getAssentoId());
        try {
            if (reserva.getStatus() != StatusReserva.CONFIRMANDO) {
                return;
            }
            assentoRepository.buscarPorId(reserva.getAssentoId()).confirmar();
            reserva.marcarConfirmada();
        } finally {
            trava.destravar(reserva.getAssentoId());
        }
    }

    public void cancelar(String reservaId) {
        Reserva reserva = reservaRepository.buscarPorId(reservaId);
        if (reserva == null) {
            throw new ReservaNaoEncontradaException(reservaId);
        }
        trava.travar(reserva.getAssentoId());
        try {
            if (reserva.getStatus() == StatusReserva.PENDENTE || reserva.getStatus() == StatusReserva.CONFIRMANDO) {
                assentoRepository.buscarPorId(reserva.getAssentoId()).liberar();
                reserva.marcarCancelada();
            }
        } finally {
            trava.destravar(reserva.getAssentoId());
        }
    }

    @Scheduled(fixedRateString = "${ticketflow.intervalo-expiracao-ms}")
    public void expirarReservasVencidas() {
        for (Reserva reserva : reservaRepository.listarTodas()) {
            if (reserva.getStatus() == StatusReserva.PENDENTE && estaExpirada(reserva)) {
                trava.travar(reserva.getAssentoId());
                try {
                    if (reserva.getStatus() == StatusReserva.PENDENTE && estaExpirada(reserva)) {
                        expirarComTravaAdquirida(reserva);
                    }
                } finally {
                    trava.destravar(reserva.getAssentoId());
                }
            }
        }
    }

    private void expirarComTravaAdquirida(Reserva reserva) {
        assentoRepository.buscarPorId(reserva.getAssentoId()).liberar();
        reserva.marcarExpirada();
    }

    private boolean estaExpirada(Reserva reserva) {
        return reserva.getExpiraEm() != null && Instant.now().isAfter(reserva.getExpiraEm());
    }

    private void enfileirar(String reservaId) {
        try {
            filaConfirmacao.put(reservaId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrompido ao enfileirar confirmação da reserva " + reservaId, e);
        }
    }

    private void dormirArtificialmente() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
