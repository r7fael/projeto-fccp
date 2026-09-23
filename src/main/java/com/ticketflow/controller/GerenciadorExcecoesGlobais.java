package com.ticketflow.controller;

import com.ticketflow.controller.dto.ErroResposta;
import com.ticketflow.excecao.AssentoNaoEncontradoException;
import com.ticketflow.excecao.AssentoOcupadoException;
import com.ticketflow.excecao.RequisicaoInvalidaException;
import com.ticketflow.excecao.ReservaExpiradaException;
import com.ticketflow.excecao.ReservaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GerenciadorExcecoesGlobais {

    @ExceptionHandler(AssentoOcupadoException.class)
    public ResponseEntity<ErroResposta> tratarAssentoOcupado(AssentoOcupadoException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResposta("ASSENTO_OCUPADO", e.getMessage()));
    }

    @ExceptionHandler({AssentoNaoEncontradoException.class, ReservaNaoEncontradaException.class})
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroResposta("NAO_ENCONTRADO", e.getMessage()));
    }

    @ExceptionHandler(ReservaExpiradaException.class)
    public ResponseEntity<ErroResposta> tratarReservaExpirada(ReservaExpiradaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroResposta("RESERVA_EXPIRADA", e.getMessage()));
    }

    @ExceptionHandler({RequisicaoInvalidaException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErroResposta> tratarRequisicaoInvalida(Exception e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(new ErroResposta("REQUISICAO_INVALIDA", e.getMessage()));
    }
}
