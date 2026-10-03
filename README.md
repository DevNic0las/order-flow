🇧🇷 **Português** | 🇺🇸 [English](./README.en.md)

# Order Flow

Sistema de pedidos distribuído orientado a eventos, construído para demonstrar **saga orquestrada por eventos, outbox pattern, idempotência, compensação e DLQ** em Java 21 + Spring Boot sobre RabbitMQ. O problema de arquitetura que ele ataca é a consistência entre serviços autônomos: cada módulo tem seu próprio schema Postgres e nunca chama o outro de forma síncrona no fluxo de negócio.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-messaging-ff6600)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-compose-2496ed)
![Azure](https://img.shields.io/badge/Azure-Container%20Apps-0078d4)
![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088ff)

## Why this project

- **Mensageria assíncrona com exchanges direct + fanout**: o resultado de um pedido é propagado a múltiplos consumidores sem acoplamento entre produtor e consumidores.
- **Saga com compensação**: o estoque é reservado, o pagamento decide e, em caso de recusa, um evento de compensação devolve o estoque reservado — cada etapa é idempotente.
- **Outbox pattern**: `Order` e `OutboxEvent` são gravados na mesma transação; um publisher agendado publica os pendentes, eliminando o caso "pedido salvo, evento nunca publicado".
- **Consistência sob concorrência**: idempotência via `INSERT ... ON CONFLICT DO NOTHING`, optimistic locking (`@Version`) em `Order` e `Inventory`, e DLQ para mensagens não processáveis.

## Architecture

```mermaid
flowchart LR
  Client[Cliente / Browser] --> GW[gateway :8080<br/>valida JWT]
  GW --> AUTH[auth-service :8084]
  GW --> ORD[order :8081]
  GW --> INV[inventory :8082]
  GW --> WEB[web :8085<br/>BFF]
  WEB --> GW

  ORD <--> MQ[(RabbitMQ)]
  INV <--> MQ
  PAY[payment-service :8086] <--> MQ
  NOT[notification :8083] <--> MQ
  AUTH --> MQ

  ORD --- DB1[(orders)]
  INV --- DB2[(inventory)]
  PAY --- DB3[(payment)]
  NOT --- DB4[(notification)]
  AUTH --- DB5[(users + email_verification)]
```

| Módulo | Responsabilidade |
|---|---|
| `gateway` (:8080) | Entrada única (Spring Cloud Gateway/WebFlux); valida assinatura/expiração do JWT antes de rotear por path. |
| `auth-service` (:8084) | Registro, login, verificação de e-mail (com cooldown de reenvio) e emissão de JWT. |
| `auth-security` | Lib compartilhada de validação de JWT e extração de roles. Sem controller próprio. |
| `order` (:8081) | Cria pedidos (outbox + `Idempotency-Key`), escuta o resultado e atualiza o status. |
| `inventory` (:8082) | Reserva/debita estoque, inicia o pagamento, aprova/rejeita e compensa reservas. |
| `payment-service` (:8086) | Consome o pedido de pagamento, autoriza (idempotente) e publica resultado/compensação. |
| `notification` (:8083) | Envia e-mail de resultado do pedido e de verificação de conta (consumidor idempotente). |
| `web` (:8085) | BFF Thymeleaf com sessão; guarda o JWT no `HttpSession` e chama o gateway como qualquer cliente. |

## Main flow

1. Cliente cria o pedido (`POST /orders`) pelo gateway; o `order` grava `Order` como `PENDING` + `OutboxEvent` na mesma transação.
2. O `OutboxPublisher` (agendado) publica o evento em `order.exchange` com routing key `rk.inventory`.
3. O `inventory` consome, checa idempotência e reserva o estoque com lock otimista. Estoque insuficiente → publica rejeição em `order.result.exchange`; reserva OK → publica pedido de pagamento em `payment.exchange`.
4. O `payment-service` consome, checa idempotência e autoriza o pagamento, publicando o resultado em `order.result.exchange` (fanout).
5. O `order` atualiza o status para `CONFIRMED`/`REJECTED` e o `notification` envia o e-mail — ambos consomem o fanout e são idempotentes.
6. **Compensação**: se o pagamento recusar, ele publica o resultado negativo e um evento em `payment.compensation.exchange`; o `inventory` consome e devolve a quantidade reservada de forma idempotente.
7. Mensagens que falham de forma não recuperável vão para a DLQ correspondente em vez de se perder.

## Key patterns

| Padrão | Onde é aplicado | Por quê |
|---|---|---|
| Outbox | `order` (`tb_outbox_events` + publisher agendado) | Atomicidade entre salvar o pedido e publicar o evento. |
| Idempotency (`ON CONFLICT DO NOTHING`) | `order`, `inventory`, `payment`, `notification` | Redelivery da mesma mensagem não duplica efeitos. |
| Saga + compensação | `inventory` ↔ `payment` | Desfazer a reserva de estoque quando o pagamento recusa. |
| Optimistic locking (`@Version`) | entidades `Order` e `Inventory` | Evitar perda de update em operações concorrentes. |
| DLQ + retry/backoff | todas as filas; retry configurado no listener de `notification` | Não perder mensagens não processáveis; absorver falhas transientes. |
| Schema per module + Flyway | todos os módulos | Isolamento de dados e migrações versionadas. |
| Defense in depth (JWT) | `gateway` + serviços downstream via `auth-security` | O token é validado na borda e novamente em cada serviço. |
| RBAC | `order` (`GET`), `inventory` (cadastro de produto) | Endpoints admin restritos a `ROLE_ADMIN`. |
| BFF com sessão | `web` | JWT fica no servidor (`HttpSession`), nunca em cookie/localStorage. |

## Tech stack

- Java 21 · Spring Boot 3.3.5 · Spring Cloud Gateway 2023.0.3
- Spring AMQP (RabbitMQ) · Spring Data JPA · Bean Validation
- PostgreSQL 16 · Flyway · um schema por módulo
- MapStruct · Lombok
- JWT via jjwt 0.12.6 (módulo `auth-security`)
- Thymeleaf (apenas `web`)
- Actuator + Micrometer (Prometheus)
- Testcontainers 2.0.5
- Maven multi-módulo · Docker · Azure Container Apps · GitHub Actions

## Observability & CI/CD

- Cada serviço expõe `health`, `info` e `prometheus` via Actuator/Micrometer. `monitoring/prometheus.yml` e `docker-compose-monitoring.yml` sobem Prometheus (`:9090`) e Grafana (`:3000`).
- **CI** (`.github/workflows/ci.yml`): `mvn clean verify` em push/PR para `main` e `develop` (Java 21 Temurin, cache Maven).
- **CD** (`.github/workflows/azure-login.yml`): em push para `main`, detecta por path-filter quais módulos mudaram, monta uma matrix e faz build/push da imagem no ACR e `az containerapp update` no Azure Container Apps. Credenciais vêm de secrets do GitHub.

## Running locally

Pré-requisitos: Docker e Docker Compose.

```bash
cp .env.example .env      # preencha JWT_SECRET (ex.: openssl rand -base64 48)
docker compose up -d
```

- Gateway: `http://localhost:8080` · Web/BFF: `http://localhost:8085`
- RabbitMQ management: `http://localhost:15672` · Mailpit (e-mails de dev): `http://localhost:8025`
- Observabilidade (opcional): `docker compose -f docker-compose-monitoring.yml up -d`

Sem `BREVO_API_KEY`, o `notification` sobe normalmente e os e-mails de desenvolvimento podem ser vistos no Mailpit.

## Project status / next steps

- A decisão de pagamento (`PaymentService.authorize`) é determinística e **sempre aprova** nesta versão; o ramo de recusa/compensação já está implementado e testado, aguardando a regra real.
- `monitoring/prometheus.yml` ainda aponta para portas/ serviços antigos (order/inventory/notification em 8080/8081/8082); os scrape targets precisam ser alinhados aos portos atuais e aos demais módulos.
- Testes de concorrência/idempotência/outbox/compensação usam Testcontainers.

## Author

Nicolas Emanuel de Sena Cajueiro (Badniko) — [GitHub @DevNic0las](https://github.com/DevNic0las)

Licença MIT — veja [LICENSE](./LICENSE).
