package com.ticketflow.controller;

import com.ticketflow.controller.dto.CriarReservaRequisicao;
import com.ticketflow.controller.dto.ReservaResposta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ReservaControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void saudeRetornaOk() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/saude", String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).contains("\"status\":\"ok\"");
    }

    @Test
    void listarAssentosRetornaOk() {
        ResponseEntity<String> resposta = restTemplate.getForEntity("/assentos", String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).contains("\"status\":\"LIVRE\"");
    }

    @Test
    void fluxoCompletoDeReserva() {
        CriarReservaRequisicao requisicao = new CriarReservaRequisicao("ana", 1);
        ResponseEntity<ReservaResposta> criada = restTemplate.postForEntity("/reservas", requisicao, ReservaResposta.class);
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criada.getBody()).isNotNull();
        assertThat(criada.getBody().status()).isEqualTo("PENDENTE");

        CriarReservaRequisicao conflitante = new CriarReservaRequisicao("bruno", 1);
        ResponseEntity<String> conflito = restTemplate.postForEntity("/reservas", conflitante, String.class);
        assertThat(conflito.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        String reservaId = criada.getBody().reservaId();
        ResponseEntity<ReservaResposta> confirmada = restTemplate.postForEntity("/reservas/" + reservaId + "/confirmar", null, ReservaResposta.class);
        assertThat(confirmada.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    @Test
    void cancelarReservaInexistenteRetorna404() {
        ResponseEntity<String> resposta = restTemplate.exchange("/reservas/r-inexistente", HttpMethod.DELETE, null, String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void cancelarReservaPendenteRetorna204() {
        CriarReservaRequisicao requisicao = new CriarReservaRequisicao("carla", 2);
        ResponseEntity<ReservaResposta> criada = restTemplate.postForEntity("/reservas", requisicao, ReservaResposta.class);
        String reservaId = criada.getBody().reservaId();

        ResponseEntity<Void> cancelada = restTemplate.exchange("/reservas/" + reservaId, HttpMethod.DELETE, null, Void.class);
        assertThat(cancelada.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void criarReservaComCampoFaltandoRetorna422() {
        String corpoInvalido = "{\"usuario\":\"ana\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requisicao = new HttpEntity<>(corpoInvalido, headers);

        ResponseEntity<String> resposta = restTemplate.postForEntity("/reservas", requisicao, String.class);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
