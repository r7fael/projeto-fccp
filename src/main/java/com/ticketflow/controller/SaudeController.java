package com.ticketflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SaudeController {

    @GetMapping("/saude")
    public Map<String, String> verificarSaude() {
        return Map.of("status", "ok");
    }
}
