# Notification Service (Phase 5)

Sends **real email** over **SMTP** (free/OSS-friendly: **Mailpit** locally, self-hosted **Postfix**/Exim, or **Gmail** with an app password — no paid API required). Consumes **`orders`**, **`payments`**, and **`deliveries`**; publishes **`notifications`** as CloudEvents.

## Features

- **SMTP email** via Spring `JavaMailSender` (`spring-boot-starter-mail`)
- **Redis idempotency** per CloudEvent `eventId` (`idempotency:notification:<eventId>`)
- **CloudEvents** on the `notifications` topic (`com.ecommerce.notification.sent` / `.failed`)
- **Recipient routing** via `notification.mail` (override, per-customer directory, or synthetic fallback address)

## Event flow (HLD)

```
orders   (com.ecommerce.order.placed)     → email: order placed
payments (com.ecommerce.payment.processed / .failed) → email: payment ok / failed
deliveries (com.ecommerce.delivery.shipped / .delivered) → email: shipped / delivered
```

Delivery payloads may omit `customerId`; use **`notification.mail.override-to`** or **`directory`** so mail goes to a real inbox.

## Local email testing (Mailpit)

1. Start stack (repo root): `docker compose up -d kafka redis notification-db mailpit`
2. Mailpit **SMTP**: `localhost:1025` — **Web UI**: http://localhost:8025  
3. Run notification-service with defaults in `application.yml` (`spring.mail.host: localhost`, `port: 1025`).

Optional: send everything to one address (e.g. your Gmail) for end-to-end demos:

```bash
export NOTIFICATION_OVERRIDE_TO='you@example.com'
```

Or map logical customer IDs in `application.yml` under `notification.mail.directory`.

## Other free SMTP options

| Option | Notes |
|--------|--------|
| **Mailpit** | OSS, included in `docker-compose.yml` |
| **Self-hosted Postfix/Exim** | Point `spring.mail.host` at your server |
| **Gmail** | Use an [App Password](https://support.google.com/accounts/answer/185833); set `spring.mail.username` / `password` and `spring.mail.properties.mail.smtp.starttls.enable=true` |

## Technology stack

- Spring Boot 4, JPA (MySQL), Kafka, Redis, JavaMail

## Kafka topics

| Direction | Topic | Notes |
|-----------|-------|--------|
| Consume | `orders` | `com.ecommerce.order.placed` |
| Consume | `payments` | `payment.processed`, `payment.failed` |
| Consume | `deliveries` | `delivery.shipped`, `delivery.delivered` |
| Produce | `notifications` | CloudEvent success/failure |

## Configuration

See `src/main/resources/application.yml` and `application-docker.yml`:

- **`spring.mail.*`** — SMTP (Mailpit in Docker: host `mailpit`, port `1025`)
- **`spring.data.redis.*`** — idempotency
- **`notification.mail.*`** — `enabled`, `from`, `override-to`, `directory`, fallbacks
- **Server port**: `8086` (avoids clash with search-service on 8085)

## Run

```bash
cd notification-service
./gradlew bootRun
```

Use profile `docker` when all dependencies run in Compose:

```bash
./gradlew bootRun --args='--spring.profiles.active=docker'
```

## Troubleshooting

- **No messages in Mailpit**: confirm Mailpit is up, `spring.mail.host`/`port` match, and `notification.mail.enabled=true`.
- **Wrong recipient**: set `NOTIFICATION_OVERRIDE_TO` or `notification.mail.directory`.
- **Duplicates**: Redis key is per `eventId`; ensure producers emit stable unique IDs.
