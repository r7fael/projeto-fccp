package com.ticketflow.controller.dto;

public record ReservaResposta(String reservaId, int assentoId, String status, Long expiraEmSegundos) {
}
