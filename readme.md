# Order Flow

Sistema de pedidos orientado a eventos, construído para praticar arquitetura de mensageria assíncrona com RabbitMQ — incluindo autenticação centralizada, API Gateway, tratamento de falhas via Dead Letter Queue (DLQ), outbox pattern, idempotência e controle de concorrência otimista.

O projeto simula um fluxo real de e-commerce: um usuário se registra e confirma o e-mail, faz login, cria um pedido, o estoque é verificado e baixado de forma assíncrona, e o cliente é notificado do resultado por e-mail — tudo desacoplado via filas, sem chamadas síncronas entre os módulos de negócio.

## Como funciona

```
Cliente → gateway (:8080)
              │  valida JWT
              ▼
   ┌──────────┼──────────────┬──────────────┐
   ▼          ▼              ▼              ▼
/auth/**  /orders/**   /inventory/**  /notifications/**
(auth-      (order-       (inventory-    (notification-
service)    service)      service)       service)


Fluxo de pedido:

  POST /orders (order-service)
        │
        ▼
  order.exchange (direct)
        │  rk.inventory
        ▼
  inventory.queue → inventory-service
        │
        │  baixa o estoque (aprova ou rejeita)
        ▼
  order.result.exchange (fanout)
    │                        │
    ▼                        ▼
order.result.queue      notification.queue
    │                        │
    ▼                        ▼
order-service atualiza   notification-service
o status do pedido       envia e-mail (Brevo)
(CONFIRMED/REJECTED)
```

1. **auth-service** cuida de registro, login e verificação de e-mail (código enviado via fila, com cooldown para reenvio) e emite o JWT.
2. **gateway** é o único ponto de entrada (porta 8080): valida o JWT em toda rota protegida (defesa em profundidade, além da validação que cada serviço já faz internamente) e roteia para o serviço correto.
3. **order-service** recebe o pedido, salva com status `PENDING` (Order + evento de outbox na mesma transação) e publica em `order.exchange`.
4. **inventory-service** consome o evento, tenta debitar a quantidade do produto (com lock otimista e checagem de idempotência) e publica o resultado (aprovado/rejeitado) em `order.result.exchange`, que é um *fanout* — todo mundo inscrito recebe.
5. **order-service** escuta o resultado (também com checagem de idempotência) e atualiza o status do pedido para `CONFIRMED` ou `REJECTED`.
6. **notification-service** também escuta o resultado e dispara um e-mail real via Brevo informando o cliente.
7. Se o consumo de uma mensagem falhar, ela vai para a fila de DLQ correspondente (`inventory.dlq` / `notification.dlq` / `order.result.dlq`) em vez de se perder.

## Decisões técnicas / diferenciais

- **Outbox pattern** no order-service: a gravação do pedido e a publicação do evento acontecem na mesma transação (via tabela de outbox + publisher agendado), eliminando o cenário de "pedido salvo mas evento nunca publicado".
- **Idempotência** em todos os consumers críticos (`inventory`, `order`, e o fluxo de verificação de e-mail), via `INSERT ... ON CONFLICT DO NOTHING` — evita processar a mesma mensagem duas vezes em caso de redelivery.
- **Lock otimista (`@Version`)** em `Order` e `Inventory`, com testes de concorrência (Testcontainers) provando que updates simultâneos não se perdem.
- **JWT com defesa em profundidade**: validado no gateway (reativo, WebFlux) e de novo em cada serviço downstream (servlet), via módulo `auth-security` compartilhado.
- **RBAC** simples (`CUSTOMER` / `ADMIN`) — ex: cadastro de produto no estoque é restrito a `ADMIN`.

## Stack

- **Java 21** + **Spring Boot 3.3.5**
- **Spring Cloud Gateway** (WebFlux/Netty) — API Gateway reativo
- **RabbitMQ** — mensageria assíncrona (exchanges direct + fanout, DLQ)
- **PostgreSQL** — um schema por módulo (`orders`, `inventory`, `users`), migrations via **Flyway**
- **Maven** multi-módulo
- **Lombok**, **Bean Validation (Jakarta)**, **MapStruct**
- **JWT (jjwt)** — autenticação stateless
- **Prometheus + Grafana** — métricas (via Actuator + Micrometer)
- **Brevo** — envio de e-mail real (verificação de conta e notificação de pedido); **Mailpit** como sandbox SMTP local para dev

## Estrutura do projeto

```
order-flow/
├── gateway/          # API Gateway (Spring Cloud Gateway) — entrada única, valida JWT
├── auth-service/      # registro, login, verificação de e-mail, emissão de JWT
├── auth-security/     # validação de JWT compartilhada entre order/inventory/gateway
├── order/             # cria pedidos, publica eventos, escuta o resultado (outbox + idempotência)
├── inventory/          # controla estoque, aprova/rejeita a baixa (lock otimista + idempotência)
├── notification/       # envia e-mail com o resultado do pedido / verificação de conta
├── monitoring/         # config do Prometheus
├── docker-compose.yml            # infra: rabbitmq, postgres, mailpit + serviços
└── docker-compose-monitoring.yml # prometheus + grafana (opcional, à parte)
```

Cada módulo de aplicação (`order`, `inventory`, `notification`, `auth-service`, `gateway`) é um projeto Spring Boot independente; `auth-security` é uma lib compartilhada. Todos unidos por um `pom.xml` pai na raiz.

## Portas

| Serviço | Porta | Acesso |
|---|---|---|
| **gateway** | 8080 | ponto de entrada único |
| order-service | 8081 | via gateway (`/orders/**`) |
| inventory-service | 8082 | via gateway (`/inventory/**`) |
| notification-service | 8083 | via gateway (`/notifications/**`) |
| auth-service | 8084 | via gateway (`/auth/**`) |
| RabbitMQ (management UI) | 15672 | direto |
| PostgreSQL | 5432 | direto |
| Mailpit (UI de e-mail, dev) | 8025 | direto |

> ⚠️ **Nota sobre o `docker-compose.yml`:** o arquivo atual ainda sobe só `order`/`inventory`/`notification` nas portas antigas (8080/8081/8082) e não inclui `gateway` nem `auth-service`. Precisa ser atualizado (+ Dockerfiles de `gateway` e `auth-service`, que ainda não existem) para refletir a arquitetura acima antes de rodar `docker compose up -d` fim a fim.

## Rodando localmente

Pré-requisitos: Docker e Docker Compose.

```bash
docker compose up -d
```

Para observabilidade (opcional):
```bash
docker compose -f docker-compose-monitoring.yml up -d
```
Prometheus em `:9090`, Grafana em `:3000` (login `admin`/`admin`).

## Fluxo de uso (via gateway, `:8080`)

### 1. Registrar usuário
```
POST /auth/register
{
  "name": "Nicolas",
  "email": "nicolas@example.com",
  "password": "senha123"
}
```
Dispara um e-mail com código de verificação (via fila, assíncrono).

### 2. Confirmar e-mail
```
POST /auth/verifycode
{
  "token": "...",
  "code": "123456"
}
```

### 3. Login
```
POST /auth/login
{
  "email": "nicolas@example.com",
  "password": "senha123"
}
```
Retorna o JWT a ser usado no header `Authorization: Bearer <token>` das próximas chamadas.

### 4. Cadastrar produto no estoque (requer role `ADMIN`)
```
POST /inventory/products
{
  "productName": "Teclado mecânico",
  "quantity": 50
}
```

### 5. Criar pedido
```
POST /orders
{
  "customerName": "Nicolas",
  "productId": 1,
  "quantity": 2
}
```
Retorna o pedido com status `PENDING`; o status final (`CONFIRMED`/`REJECTED`) é atualizado de forma assíncrona conforme o resultado da baixa de estoque, e um e-mail é enviado ao cliente.

Todos os endpoints validam os campos de entrada (Bean Validation) e retornam `400` com o detalhe do erro por campo em caso de dado inválido.

## Testes

```bash
./mvnw clean verify
```
Roda os testes de todos os módulos (unitários de service/controller + integração com Testcontainers para RabbitMQ e concorrência). O CI (GitHub Actions) executa o mesmo comando em push/PR para `main` e `develop`.

## Limitações conhecidas / próximos passos

- `docker-compose.yml` desatualizado em relação à arquitetura atual (ver nota acima) — falta incluir `gateway`/`auth-service` e corrigir as portas.
- CI cobre os testes, mas ainda não há CD.
- Observabilidade (Prometheus/Grafana) está com dashboards básicos, ainda a evoluir.

## Licença

MIT — veja [LICENSE](./LICENSE).
