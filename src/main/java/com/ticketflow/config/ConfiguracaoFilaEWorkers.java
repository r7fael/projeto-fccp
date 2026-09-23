package com.ticketflow.config;

import com.ticketflow.service.ReservaService;
import com.ticketflow.worker.ConfirmadorPagamento;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class ConfiguracaoFilaEWorkers {

    private final int quantidadeTrabalhadores;

    private ExecutorService poolTrabalhadores;
    private List<ConfirmadorPagamento> trabalhadores;
    private BlockingQueue<String> filaConfirmacao;

    public ConfiguracaoFilaEWorkers(@Value("${ticketflow.quantidade-trabalhadores}") int quantidadeTrabalhadores) {
        this.quantidadeTrabalhadores = quantidadeTrabalhadores;
    }

    @Bean
    public BlockingQueue<String> filaConfirmacao() {
        this.filaConfirmacao = new LinkedBlockingQueue<>();
        return this.filaConfirmacao;
    }

    @Bean
    public List<ConfirmadorPagamento> trabalhadoresConfirmacao(BlockingQueue<String> filaConfirmacao, ReservaService reservaService) {
        this.poolTrabalhadores = Executors.newVirtualThreadPerTaskExecutor();
        this.trabalhadores = new ArrayList<>();
        for (int i = 0; i < quantidadeTrabalhadores; i++) {
            ConfirmadorPagamento trabalhador = new ConfirmadorPagamento(filaConfirmacao, reservaService);
            trabalhadores.add(trabalhador);
            poolTrabalhadores.submit(trabalhador);
        }
        return trabalhadores;
    }

    @PreDestroy
    public void encerrarTrabalhadores() {
        if (trabalhadores == null) {
            return;
        }
        for (int i = 0; i < trabalhadores.size(); i++) {
            filaConfirmacao.add(ConfirmadorPagamento.PILULA_ENVENENADA);
        }
        poolTrabalhadores.close();
    }
}
