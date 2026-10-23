# Backend

Server-side application source code lives in `src/`.

## Demo Data

The `demo` Spring profile automatically loads `src/main/resources/demo/seed.sql` into PostgreSQL at startup.

**Warning:** Every demo startup clears and reseeds all 12 tables. Use a dedicated demo database/branch. Normal startup preserves existing data.

### Setup

1. Copy `backend/.env.example` to `backend/.env` and configure the database credentials.
2. Ensure the database schema already exists.
3. From `tender-app/backend`, run:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Alternatively, add `--spring.profiles.active=demo` to your IDE's run configuration.

### Seeded Data

Includes 9 users, 3 restaurants, 7 menu items, and 8 orders, covering REG/ORD scenarios such as restaurant availability, sold out items, order lifecycles, and preparation queues.

**Demo accounts:**

| Role | Email |
-----------------------------
| Customer | alice@demo.test |
| Staff | sam@demo.test |
| Courier | leo@demo.test |
| Operator | operator@demo.test |

**Password:** `DemoPass123!` (verify BCrypt compatibility when implementing authentication).

**Remove the `demo` profile to run without resetting the database.**