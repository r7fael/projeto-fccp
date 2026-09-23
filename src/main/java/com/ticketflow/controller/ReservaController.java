package com.ticketflow.controller;

import com.ticketflow.controller.dto.CriarReservaRequisicao;
import com.ticketflow.controller.dto.ReservaResposta;
import com.ticketflow.excecao.RequisicaoInvalidaException;
import com.ticketflow.model.Reserva;
import com.ticketflow.service.ReservaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<ReservaResposta> criar(@RequestBody CriarReservaRequisicao requisicao) {
        String usuario = requisicao.usuario();
        Integer assentoId = requisicao.assentoId();
        if (usuario == null || usuario.isBlank()) {
            throw new RequisicaoInvalidaException("Campo obrigatório ausente ou inválido: usuario");
        }
        if (assentoId == null) {
            throw new RequisicaoInvalidaException("Campo obrigatório ausente ou inválido: assentoId");
        }

        Reserva reserva = reservaService.reservar(usuario, assentoId);
        long expiraEmSegundos = Duration.between(Instant.now(), reserva.getExpiraEm()).getSeconds();
        ReservaResposta corpo = new ReservaResposta(reserva.getId(), reserva.getAssentoId(), reserva.getStatus().name(), expiraEmSegundos);
        return ResponseEntity.status(HttpStatus.CREATED).body(corpo);
    }

    @PostMapping("/{id}/confirmar")
    public ResponseEntity<ReservaResposta> confirmar(@PathVariable("id") String reservaId) {
        Reserva reserva = reservaService.confirmar(reservaId);
        ReservaResposta corpo = new ReservaResposta(reserva.getId(), reserva.getAssentoId(), reserva.getStatus().name(), null);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(corpo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable("id") String reservaId) {
        reservaService.cancelar(reservaId);
        return ResponseEntity.noContent().build();
    }
}
