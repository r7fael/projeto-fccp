package com.ticketflow.controller.dto;

import java.time.Instant;

public record AssentoResposta(int id, String status, String usuario, Instant expiraEm) {
}
