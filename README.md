# Order Flow

Sistema de pedidos orientado a eventos, construído para praticar arquitetura de mensageria assíncrona com RabbitMQ, incluindo tratamento de falhas via Dead Letter Queue (DLQ).

O projeto simula um fluxo real de e-commerce: um pedido é criado, o estoque é verificado e baixado de forma assíncrona, e o cliente é notificado do resultado — tudo desacoplado via filas, sem chamadas síncronas entre os módulos.

## Como funciona

```
Cliente → POST /orders (order-service)
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
  order-service atualiza    notification-service
  o status do pedido        envia e-mail ao cliente
  (CONFIRMED/REJECTED)
```

1. **order-service** recebe o pedido, salva com status `PENDING` e publica um evento em `order.exchange`.
2. **inventory-service** consome o evento, tenta debitar a quantidade do produto e publica o resultado (aprovado/rejeitado) em `order.result.exchange`, que é um *fanout* — ou seja, todo mundo inscrito recebe.
3. **order-service** escuta o resultado e atualiza o status do pedido para `CONFIRMED` ou `REJECTED`.
4. **notification-service** também escuta o resultado e dispara um e-mail informando o cliente.
5. Se o consumo de uma mensagem falhar, ela vai para a fila correspondente de DLQ (`inventory.dlq` / `notification.dlq`) em vez de se perder.

## Stack

- **Java 21** + **Spring Boot 3.3.5**
- **RabbitMQ** — mensageria assíncrona (exchanges direct + fanout, DLQ)
- **PostgreSQL** — um schema por módulo (`orders`, `inventory`), migrations via **Flyway**
- **Maven** multi-módulo
- **Lombok**, **Bean Validation (Jakarta)**
- **Prometheus + Grafana** — métricas (via Actuator + Micrometer)
- **Mailpit** — servidor SMTP local para testar envio de e-mail sem mandar e-mail de verdade

## Estrutura do projeto

```
order-flow/
├── order/          # cria pedidos, publica eventos, escuta o resultado
├── inventory/       # controla estoque, aprova/rejeita a baixa
├── notification/     # envia e-mail com o resultado do pedido
├── monitoring/       # config do Prometheus
├── docker-compose.yml            # infra: rabbitmq, postgres, mailpit + serviços
└── docker-compose-monitoring.yml # prometheus + grafana (opcional, à parte)
```

Cada módulo é um projeto Spring Boot independente (`order`, `inventory`, `notification`), unidos por um `pom.xml` pai na raiz.

## Rodando localmente

Pré-requisitos: Docker e Docker Compose.

```bash
docker compose up -d
```

Isso sobe:
| Serviço | Porta |
|---|---|
| order-service | 8080 |
| inventory-service | 8081 |
| notification-service | 8082 |
| RabbitMQ (management UI) | 15672 (user/senha: `guest`/`guest`) |
| PostgreSQL | 5432 |
| Mailpit (UI de e-mail) | 8025 |

> Cada serviço usa `context: .` (raiz do projeto) com `dockerfile: <módulo>/Dockerfile`, porque o `pom.xml` de cada módulo depende do `pom.xml` pai — o build precisa enxergar o projeto multi-módulo inteiro, não só a pasta do serviço.

Para observabilidade (opcional):
```bash
docker compose -f docker-compose-monitoring.yml up -d
```
Prometheus em `:9090`, Grafana em `:3000` (login `admin`/`admin`).

## Endpoints

### Cadastrar produto no estoque
```
POST /inventory
{
  "productName": "Teclado mecânico",
  "quantity": 50
}
```

### Criar pedido
```
POST /orders
{
  "customerName": "Nicolas",
  "productId": 1,
  "quantity": 2
}
```
Retorna o pedido com status `PENDING`; o status final (`CONFIRMED`/`REJECTED`) é atualizado de forma assíncrona conforme o resultado da baixa de estoque.

Ambos os endpoints validam os campos de entrada (Bean Validation) e retornam `400` com o detalhe do erro por campo em caso de dado inválido.

## Testes

```bash
./mvnw clean verify
```
Roda os testes de todos os módulos (unitários de service e de controller). O CI (GitHub Actions) executa o mesmo comando em push/PR para `main` e `develop`.

## Limitações conhecidas / próximos passos

- **Notification** usa Mailpit (sandbox local) — não envia e-mail de verdade. Para produção, trocar por um provedor SMTP real.
- Observabilidade (Prometheus/Grafana) está com dashboards básicos, ainda a evoluir.

## Licença

MIT — veja [LICENSE](./LICENSE).
