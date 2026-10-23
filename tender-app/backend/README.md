# Backend

Server-side application source code lives in `src/`.

## Run locally

From this directory, start the backend with:

```bash
./mvnw spring-boot:run
```

The default `local` profile uses an in-memory H2 database and seeds a demo
restaurant (`restaurantId=1`) with orders in `PLACED`, `ACCEPTED`, and
`PREPARING` states. Data is reset when the backend restarts. To use the
configured PostgreSQL database instead, provide `DB_URL`, `DB_USER`, and
`DB_PASSWORD`, then start with `SPRING_PROFILES_ACTIVE=prod`.

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

The current project has no authentication/authorization integration. The local
restaurant profile is a demo context, not an authenticated account. Actions
verify that the order belongs to the restaurant ID in the URL, but this is not
an authenticated restaurant identity check; secure restaurant-only access
requires the project's authentication system to be added separately.
