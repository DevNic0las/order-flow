🇧🇷 [Português](./README.md) | 🇺🇸 **English**

# Order Flow

Event-driven distributed order system built to demonstrate **choreographed saga, outbox pattern, idempotency, compensation and DLQ** in Java 21 + Spring Boot on RabbitMQ. The architectural problem it tackles is consistency across autonomous services: each module owns its own Postgres schema and never calls another service synchronously in the business flow.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-messaging-ff6600)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![Docker](https://img.shields.io/badge/Docker-compose-2496ed)
![Azure](https://img.shields.io/badge/Azure-Container%20Apps-0078d4)
![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-2088ff)

## Why this project

- **Asynchronous messaging with direct + fanout exchanges**: an order result is propagated to multiple consumers without coupling producer to consumers.
- **Choreographed saga with compensation**: no central orchestrator; each service reacts to events. Inventory is reserved, payment decides, and on rejection a compensation event restores the reserved stock — every step is idempotent.
- **Outbox pattern**: `Order` and `OutboxEvent` are written in the same transaction; a scheduled publisher sends pending rows, eliminating the "order saved, event never published" gap.
- **Consistency under concurrency**: idempotency via `INSERT ... ON CONFLICT DO NOTHING`, optimistic locking (`@Version`) on `Order` and `Inventory`, and DLQ for unrecoverable messages.

## Architecture

```mermaid
flowchart LR
  Client[Client / Browser] --> WEB[web :8085<br/>BFF — only exposed entry]
  WEB --> GW[gateway :8080<br/>internal, validates JWT]

  GW --> AUTH[auth-service :8084]
  GW --> ORD[order :8081]
  GW --> INV[inventory :8082]
  GW --> NOT[notification :8083]

  ORD <--> MQ[(RabbitMQ)]
  INV <--> MQ
  PAY[payment-service :8086] <--> MQ
  NOT <--> MQ
  AUTH --> MQ

  ORD --- DB1[(orders)]
  INV --- DB2[(inventory)]
  PAY --- DB3[(payment)]
  NOT --- DB4[(notification)]
  AUTH --- DB5[(users + email_verification)]
```

| Module | Responsibility |
|---|---|
| `web` (:8085) | Thymeleaf session-based BFF, **the only entry exposed to the client**; stores the JWT in the `HttpSession` and calls the gateway as an internal client. |
| `gateway` (:8080) | Single entry point for internal services (Spring Cloud Gateway/WebFlux); validates JWT signature/expiration before path-routing. |
| `auth-service` (:8084) | Registration, login, email verification (with resend cooldown) and JWT issuance. |
| `auth-security` | Shared JWT validation/role extraction library. No controller of its own. |
| `order` (:8081) | Creates orders (outbox + `Idempotency-Key`), listens for the result and updates status. |
| `inventory` (:8082) | Reserves/debits stock, starts payment, approves/rejects and compensates reservations. |
| `payment-service` (:8086) | Consumes the payment request, authorizes (idempotent) and publishes result/compensation. |
| `notification` (:8083) | Sends order-result and account-verification emails (idempotent consumer). |

## Main flow

1. The order enters through `web` (BFF), which calls the `gateway` with `POST /orders` and the `Idempotency-Key` header; `order` persists `Order` as `PENDING` + `OutboxEvent` in the same transaction.
2. The scheduled `OutboxPublisher` publishes the event to `order.exchange` with routing key `rk.inventory`.
3. `inventory` consumes it, checks idempotency and reserves stock with optimistic locking. Insufficient stock → publishes a rejection to `order.result.exchange`; reservation OK → publishes a payment request to `payment.exchange`.
4. `payment-service` consumes it, checks idempotency and authorizes payment, publishing the result to `order.result.exchange` (fanout).
5. `order` updates the status to `CONFIRMED`/`REJECTED` and `notification` sends the email — both consume the fanout and are idempotent.
6. **Compensation**: if payment rejects, it publishes the negative result and an event to `payment.compensation.exchange`; `inventory` consumes it and restores the reserved quantity idempotently.
7. Messages that fail unrecoverably go to the corresponding DLQ instead of being lost.

## Key patterns

| Pattern | Where it is applied | Why |
|---|---|---|
| Outbox | `order` (`tb_outbox_events` + scheduled publisher) | Atomicity between saving the order and publishing the event. |
| Idempotency (`ON CONFLICT DO NOTHING`) | Consumers and `POST /orders` (`Idempotency-Key` header) | Redelivery of the same message (or client retry) does not duplicate effects. |
| Processed-event tables | `tb_processed_order_result_events` (`order`), `tb_processed_inventory_events` + `tb_processed_payment_compensation_events` (`inventory`), `tb_processed_payment_events` (`payment`), `tb_processed_notification_events` (`notification`) | Record `event_id` atomically to guarantee idempotency. |
| `POST /orders` Idempotency-Key | `order` (`tb_order_idempotency_keys`) + `Idempotency-Key` header | A client retry returns the same order instead of creating another. |
| Choreographed saga + compensation | `inventory` ↔ `payment` | Undo the stock reservation when payment rejects, with no orchestrator. |
| Optimistic locking (`@Version`) | `Order` and `Inventory` entities | Prevent lost updates on concurrent operations. |
| DLQ + retry/backoff | every queue; retry configured on the `notification` listener | Avoid losing unrecoverable messages; absorb transient failures. |
| Schema per module + Flyway | all modules | Data isolation and versioned migrations. |
| Defense in depth (JWT) | `gateway` + downstream services via `auth-security` | Token is validated at the edge and again on each service. |
| RBAC | `order` (`GET`), `inventory` (product creation) | Admin endpoints restricted to `ROLE_ADMIN`. |
| Session BFF | `web` | JWT stays server-side (`HttpSession`), never in a cookie/localStorage. |

## Tech stack

- Java 21 · Spring Boot 3.3.5 · Spring Cloud Gateway 2023.0.3
- Spring AMQP (RabbitMQ) · Spring Data JPA · Bean Validation
- PostgreSQL 16 · Flyway · one schema per module
- MapStruct · Lombok
- JWT via jjwt 0.12.6 (`auth-security` module)
- Thymeleaf (`web` only)
- Actuator + Micrometer (Prometheus)
- Testcontainers 2.0.5
- Maven multi-module · Docker · Azure Container Apps · GitHub Actions

## Observability & CI/CD

- Every service exposes `health`, `info` and `prometheus` via Actuator/Micrometer. `monitoring/prometheus.yml` and `docker-compose-monitoring.yml` bring up Prometheus (`:9090`) and Grafana (`:3000`).
- **CI** (`.github/workflows/ci.yml`): `mvn clean verify` on push/PR to `main` and `develop` (Java 21 Temurin, Maven cache).
- **CD** (`.github/workflows/azure-login.yml`): on push to `main`, `dorny/paths-filter` detects which modules changed (changes to `auth-security` redeploy the services that consume it; changes to the root `pom.xml`, all of them) and builds a matrix; each changed service is built, has its image published to ACR with **tag equal to the commit SHA** (`:<github.sha>`) and gets `az containerapp update` on Azure Container Apps. Azure login uses **federated OIDC** (`azure/login` with `id-token: write` and the `AZURE_CLIENT_ID`/`AZURE_TENANT_ID`/`AZURE_SUBSCRIPTION_ID` secrets), with no long-lived credentials.

## Running locally

Requirements: Docker and Docker Compose.

```bash
cp .env.example .env      # set JWT_SECRET (e.g. openssl rand -base64 48)
docker compose up -d
```

- Gateway: `http://localhost:8080` · Web/BFF: `http://localhost:8085`
- RabbitMQ management: `http://localhost:15672` · Mailpit (dev emails): `http://localhost:8025`
- Observability (optional): `docker compose -f docker-compose-monitoring.yml up -d`

Without `BREVO_API_KEY`, `notification` still starts and development emails can be inspected in Mailpit.

## Project status / next steps

- The payment is **simulated**: `PaymentService.authorize` is deterministic and **always approves** (no real PSP integration). The rejection/compensation branch is implemented and covered by tests (via override in the test), ready for the real rule.
- `monitoring/prometheus.yml` scrapes the current services on the correct ports.
- Concurrency/idempotency/outbox/compensation tests use Testcontainers.

## Author

Nicolas Emanuel de Sena Cajueiro (Badniko) — [GitHub @DevNic0las](https://github.com/DevNic0las)

MIT License — see [LICENSE](./LICENSE).
