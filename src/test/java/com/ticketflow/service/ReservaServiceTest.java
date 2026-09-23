package com.ticketflow.service;

import com.ticketflow.excecao.AssentoNaoEncontradoException;
import com.ticketflow.excecao.AssentoOcupadoException;
import com.ticketflow.excecao.ReservaExpiradaException;
import com.ticketflow.excecao.ReservaNaoEncontradaException;
import com.ticketflow.model.Assento;
import com.ticketflow.model.Reserva;
import com.ticketflow.model.StatusAssento;
import com.ticketflow.model.StatusReserva;
import com.ticketflow.repository.AssentoRepository;
import com.ticketflow.repository.ReservaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservaServiceTest {

    private ReservaService novoServico(long ttlSegundos) {
        return new ReservaService(
                new AssentoRepository(5),
                new ReservaRepository(),
                new LinkedBlockingQueue<>(),
                ModoSincronizacao.TRAVA_POR_ASSENTO,
                ttlSegundos
        );
    }

    private Assento buscarAssento(ReservaService servico, int id) {
        return servico.listarAssentos().stream()
                .filter(a -> a.getId() == id)
                .findFirst()
                .orElseThrow();
    }

    @Test
    void reservarAssentoLivreDeixaOStatusReservado() {
        ReservaService servico = novoServico(30);
        Reserva reserva = servico.reservar("ana", 3);

        assertThat(reserva.getAssentoId()).isEqualTo(3);
        assertThat(reserva.getStatus()).isEqualTo(StatusReserva.PENDENTE);
        assertThat(buscarAssento(servico, 3).getStatus()).isEqualTo(StatusAssento.RESERVADO);
        assertThat(buscarAssento(servico, 3).getUsuario()).isEqualTo("ana");
    }

    @Test
    void reservarAssentoJaOcupadoLancaExcecao() {
        ReservaService servico = novoServico(30);
        servico.reservar("ana", 3);
        assertThrows(AssentoOcupadoException.class, () -> servico.reservar("bruno", 3));
    }

    @Test
    void reservarAssentoInexistenteLancaExcecao() {
        ReservaService servico = novoServico(30);
        assertThrows(AssentoNaoEncontradoException.class, () -> servico.reservar("ana", 999));
    }

    @Test
    void confirmarReservaInexistenteLancaExcecao() {
        ReservaService servico = novoServico(30);
        assertThrows(ReservaNaoEncontradaException.class, () -> servico.confirmar("r-inexistente"));
    }

    @Test
    void confirmarMovePraConfirmandoEWorkerCompleta() {
        ReservaService servico = novoServico(30);
        Reserva reserva = servico.reservar("ana", 3);

        Reserva confirmando = servico.confirmar(reserva.getId());
        assertThat(confirmando.getStatus()).isEqualTo(StatusReserva.CONFIRMANDO);

        servico.completarConfirmacao(reserva.getId());
        assertThat(buscarAssento(servico, 3).getStatus()).isEqualTo(StatusAssento.VENDIDO);
    }

    @Test
    void cancelarLiberaOAssento() {
        ReservaService servico = novoServico(30);
        Reserva reserva = servico.reservar("ana", 3);

        servico.cancelar(reserva.getId());

        assertThat(buscarAssento(servico, 3).getStatus()).isEqualTo(StatusAssento.LIVRE);
        assertThat(buscarAssento(servico, 3).getUsuario()).isNull();
    }

    @Test
    void cancelarDuranteConfirmacaoImpedeQueOWorkerCompleteDepois() {
        ReservaService servico = novoServico(30);
        Reserva reserva = servico.reservar("ana", 3);
        servico.confirmar(reserva.getId());

        servico.cancelar(reserva.getId());
        servico.completarConfirmacao(reserva.getId());

        assertThat(buscarAssento(servico, 3).getStatus()).isEqualTo(StatusAssento.LIVRE);
    }

    @Test
    void varreduraDeExpiracaoLiberaReservaVencidaEImpedeConfirmacao() throws InterruptedException {
        ReservaService servico = novoServico(0);
        Reserva reserva = servico.reservar("ana", 3);
        Thread.sleep(50);
        servico.expirarReservasVencidas();

        assertThrows(ReservaExpiradaException.class, () -> servico.confirmar(reserva.getId()));
        assertThat(buscarAssento(servico, 3).getStatus()).isEqualTo(StatusAssento.LIVRE);
    }

    @Test
    void confirmarReservaVencidaSemVarreduraPreviaTambemExpiraNaHora() throws InterruptedException {
        ReservaService servico = novoServico(0);
        Reserva reserva = servico.reservar("ana", 3);
        Thread.sleep(50);

        assertThrows(ReservaExpiradaException.class, () -> servico.confirmar(reserva.getId()));
        assertThat(buscarAssento(servico, 3).getStatus()).isEqualTo(StatusAssento.LIVRE);
    }

    @Test
    void listarAssentosRetornaTodosOrdenadosPorId() {
        ReservaService servico = novoServico(30);
        List<Assento> assentos = servico.listarAssentos();
        assertThat(assentos).hasSize(5);
        assertThat(assentos.get(0).getId()).isEqualTo(1);
        assertThat(assentos.get(4).getId()).isEqualTo(5);
    }
}
