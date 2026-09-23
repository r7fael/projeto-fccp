# TicketFlow

Protótipo single-node de reserva de assentos com concorrência real, em Java 21 + Spring Boot 4. Veja `docs/DESIGN.md` para a arquitetura completa.

## Requisitos

- JDK 21+ (`java -version`)
- Internet na primeira execução (o `mvnw`/`mvnw.cmd` baixa o Maven e as dependências sozinho — não precisa instalar Maven)

## Rodando

```powershell
# Windows
.\mvnw.cmd spring-boot:run
```

```bash
# Mac/Linux
./mvnw spring-boot:run
```

Servidor sobe em `http://localhost:8080`.

## Testes

```powershell
.\mvnw.cmd test
```

20 testes: regras de negócio (`ReservaServiceTest`), prova de concorrência com 300 threads (`ConcorrenciaTest`) e integração HTTP de ponta a ponta (`ReservaControllerTest`).

## Configuração

Em `src/main/resources/application.properties` ou por variável de ambiente (binding automático do Spring, ex: `TICKETFLOW_MODO_SINCRONIZACAO`):

| Propriedade | Padrão | Valores |
|---|---|---|
| `server.port` | `8080` | |
| `ticketflow.modo-sincronizacao` | `TRAVA_POR_ASSENTO` | `SEM_TRAVA`, `TRAVA_GLOBAL`, `TRAVA_POR_ASSENTO` |
| `ticketflow.quantidade-assentos` | `50` | |
| `ticketflow.quantidade-trabalhadores` | `4` | |
| `ticketflow.ttl-reserva-segundos` | `30` | |
| `ticketflow.intervalo-expiracao-ms` | `1000` | |

## Demonstração de concorrência

Com o servidor rodando (`TICKETFLOW_MODO_SINCRONIZACAO=SEM_TRAVA .\mvnw.cmd spring-boot:run` para ver o bug), dispare requisições concorrentes contra `POST /reservas` para o mesmo `assentoId` de várias origens (curl em loop, Apache Bench, etc.) e observe quantas retornam 201 — no modo `SEM_TRAVA` mais de uma terá sucesso para o mesmo assento; nos modos com trava, sempre exatamente uma.

A prova automatizada e determinística disso é o teste `ConcorrenciaTest` (`.\mvnw.cmd test`), que já roda os 3 modos com 300 threads reais.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/saude` | Status do serviço |
| GET | `/assentos` | Lista todos os assentos |
| POST | `/reservas` | `{"usuario": "ana", "assentoId": 3}` |
| POST | `/reservas/{id}/confirmar` | Confirma o pagamento |
| DELETE | `/reservas/{id}` | Cancela a reserva |

## Arquitetura

```
com.ticketflow/
├── model/        Assento, StatusAssento, Reserva, StatusReserva
├── excecao/       Exceções de domínio
├── repository/    AssentoRepository, ReservaRepository (em memória)
├── service/       ReservaService (regras + trava + expiração agendada), ModoSincronizacao, TravaAssento (+ 3 estratégias)
├── worker/        ConfirmadorPagamento
├── controller/    AssentoController, ReservaController, SaudeController, GerenciadorExcecoesGlobais
│   └── dto/       Records de requisição/resposta
└── config/        ConfiguracaoFilaEWorkers
```
