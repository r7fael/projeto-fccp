package com.ticketflow.service;

import com.ticketflow.repository.AssentoRepository;
import com.ticketflow.repository.ReservaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ConcorrenciaTest {

    private static final int QUANTIDADE_THREADS = 300;
    private static final int ASSENTO_ID = 1;

    @Test
    @Timeout(20)
    void modoSemTravaPermiteOverbooking() throws InterruptedException {
        int sucessos = disputarMesmoAssento(ModoSincronizacao.SEM_TRAVA);
        System.out.println("[SEM_TRAVA] reservas bem-sucedidas: " + sucessos + " (correto seria 1)");
        assertThat(sucessos).isGreaterThan(1);
    }

    @Test
    @Timeout(20)
    void travaGlobalImpedeOverbooking() throws InterruptedException {
        int sucessos = disputarMesmoAssento(ModoSincronizacao.TRAVA_GLOBAL);
        System.out.println("[TRAVA_GLOBAL] reservas bem-sucedidas: " + sucessos);
        assertThat(sucessos).isEqualTo(1);
    }

    @Test
    @Timeout(20)
    void travaPorAssentoImpedeOverbooking() throws InterruptedException {
        int sucessos = disputarMesmoAssento(ModoSincronizacao.TRAVA_POR_ASSENTO);
        System.out.println("[TRAVA_POR_ASSENTO] reservas bem-sucedidas: " + sucessos);
        assertThat(sucessos).isEqualTo(1);
    }

    private int disputarMesmoAssento(ModoSincronizacao modo) throws InterruptedException {
        ReservaService servico = new ReservaService(
                new AssentoRepository(1),
                new ReservaRepository(),
                new LinkedBlockingQueue<>(),
                modo,
                30
        );
        AtomicInteger sucessos = new AtomicInteger();
        CountDownLatch prontos = new CountDownLatch(QUANTIDADE_THREADS);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch concluidos = new CountDownLatch(QUANTIDADE_THREADS);

        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < QUANTIDADE_THREADS; i++) {
                int indiceUsuario = i;
                pool.submit(() -> {
                    prontos.countDown();
                    try {
                        largada.await();
                        servico.reservar("usuario-" + indiceUsuario, ASSENTO_ID);
                        sucessos.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException esperadoQuandoPerdeuACorrida) {
                    } finally {
                        concluidos.countDown();
                    }
                });
            }
            prontos.await();
            largada.countDown();
            concluidos.await();
        }
        return sucessos.get();
    }
}
