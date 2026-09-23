package com.ticketflow.controller;

import com.ticketflow.controller.dto.AssentoResposta;
import com.ticketflow.service.ReservaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssentoController {

    private final ReservaService reservaService;

    public AssentoController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping("/assentos")
    public List<AssentoResposta> listar() {
        return reservaService.listarAssentos().stream()
                .map(assento -> new AssentoResposta(
                        assento.getId(),
                        assento.getStatus().name(),
                        assento.getUsuario(),
                        assento.getExpiraEm()))
                .toList();
    }
}
