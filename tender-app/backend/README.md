# Backend

Server-side application source code lives in `src/`.

## Run locally

From this directory, start the backend with:

```bash
./mvnw spring-boot:run
```

By default the backend connects to the PostgreSQL database configured by
`DB_URL`, `DB_USER`, and `DB_PASSWORD`.

To work offline, run with the `local` profile instead:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The `local` profile uses an in-memory H2 database and seeds a demo
restaurant (`restaurantId=1`) with orders in `PLACED`, `ACCEPTED`, and
`PREPARING` states. Data (including accounts) is reset when the backend restarts.

## Restaurant order queues

`GET /api/restaurants/{restaurantId}/orders/preparation` returns that restaurant's
orders in `ACCEPTED` or `PREPARING` status, ordered by `acceptedAt` from oldest
to newest.

`GET /api/restaurants/{restaurantId}/orders/incoming` returns that restaurant's
`PLACED` orders, ordered by `created_at` from oldest to newest. Each order
includes its ID, status, placement time, order lines with menu item names,
quantities, and unit prices, and a total calculated from those lines. An empty
queue is returned as an empty JSON array. Hibernate validates the existing
schema and does not create or update tables.

`GET /api/restaurants/{restaurantId}/orders/pickup` returns `READY` orders that
are awaiting courier or customer pickup, ordered by placement time.

Restaurant order actions are `POST /api/restaurants/{restaurantId}/orders/{orderId}/accept`,
`/reject` (JSON body `{"reason":"..."}`), `/cancel`, `/prepare`, `/ready`,
`/picked-up`, `/delivered`, and `/refund`. The lifecycle is
`PLACED → ACCEPTED → PREPARING → READY → PICKED_UP → DELIVERED`.
`READY`, `PICKED_UP`, or `DELIVERED` orders may transition to `REFUNDED`.
`PLACED → REJECTED|CANCELLED` are terminal alternatives. Accepting records the
acceptance time. The incoming queue is ordered by placement time, while the
preparation queue (accepted and preparing orders) is ordered by acceptance time.
Rejections require a nonblank reason. Every successful action is logged to the
application console with the order ID and, for rejections, the reason.

Order status changes are published as `OrderStatusEvent` notifications to
registered `OrderObserver` implementations. `OrderRecord` preserves the
existing console event log, while `DashboardObserver` streams changes to
subscribed web dashboards through
`GET /api/restaurants/{restaurantId}/orders/events` (Server-Sent Events). The
dashboard refreshes its selected queue when it receives an event; manual
refresh and the existing lifecycle actions remain available.

The current project has no authentication/authorization integration. The local
restaurant profile is a demo context, not an authenticated account. Actions
verify that the order belongs to the restaurant ID in the URL, but this is not
an authenticated restaurant identity check; secure restaurant-only access
requires the project's authentication system to be added separately.
## Existing database upgrade

Before starting this merged version against an existing PostgreSQL database,
apply [`docs/order-lifecycle-merge.sql`](docs/order-lifecycle-merge.sql). It
allows the `refunded` lifecycle status without changing order rows or timestamps.
Apply [`docs/restaurant-profile.sql`](docs/restaurant-profile.sql) before deploying
restaurant contact profiles to an existing database.
The test suite uses an in-memory H2 database and does not apply this script to
your configured database.

## Demo Data

The `demo` Spring profile automatically loads `src/main/resources/demo/seed.sql` into PostgreSQL at startup.

**Warning:** Every demo startup clears and reseeds the 15 tables listed in the seed. Use a dedicated demo database/branch. Normal startup preserves existing data.

### Setup

1. Copy `backend/.env.example` to `backend/.env` and configure the database credentials.
2. Ensure the database schema already exists.
3. From `tender-app/backend`, run:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Alternatively, add `--spring.profiles.active=demo` to your IDE's run configuration.

### Seeded Data

Includes 9 users, 4 restaurants, 7 menu items, and 8 orders, covering REG/ORD scenarios such as restaurant availability, sold out items, order lifecycles, and preparation queues.

**Demo accounts:**

| Role | Email |
-----------------------------
| Customer | alice@demo.test |
| Staff | sam@demo.test |
| Courier | leo@demo.test |
| Operator | operator@demo.test |

**Password:** `DemoPass123!` (verify BCrypt compatibility when implementing authentication).

**Remove the `demo` profile to run without resetting the database.**

### Menu photos

The SQL seed uses the same seven dishes and image files as the frontend mocks.
Maven copies `../frontend/src/mocks/menu-items-imgs/*.jpg` into the backend's
static resources when building, so the images are included in the packaged JAR.
Unauthenticated GET requests can load `/menu-items-imgs/<filename>.jpg`.

Seeded `photo_url` values are full backend URLs, for example
`http://localhost:8080/menu-items-imgs/truffle.jpg`. These work when the browser
and backend run on the same computer. For a phone, another computer, or deployment,
replace the `http://localhost:8080` prefix in the seed with the reachable backend
origin before seeding. The images require the backend to be running and are not
uploaded to Supabase Storage.

The seed matches the public Supabase schema inspected on October 29, 2026:
menus have no name column, restaurants use `opening_hour` and `closing_hour`,
and `operator_profiles.user_id` references `users.id`. Olivia's operator profile
links to demo user 8. No table creation or schema migration is performed by the seed.
Maple & Main and Little Italy Pizza open at 09:00 and close at 22:00; Green Bowl
Kitchen and Café Nord open at 09:00 and close at 17:00. These stored hours do not
implement automatic opening/closing behavior.

The reset list also includes `order_items` because its foreign key references
orders, and `auth_sessions` to remove stale persisted demo sessions. Those two
tables are cleared but not populated. The `health_check` table is preserved.
The current schema snapshot is recorded in [docs/schema.txt](docs/schema.txt).
The backend maps its Java `dateOrdered` field to `orders.created_at`, preserving
the existing API field name. Dashboard queries also read `created_at`, so there
is no duplicate `date_ordered` column. The lifecycle migration expands the status
constraint to include `refunded`; it does not perform refunds or reseed data.

The demo seed retains open, paused, and closed restaurant scenarios, so its
restaurant statuses and generated IDs can differ from the frontend mock state.
