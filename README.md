<h1 align="center" style="font-family: 'Lexend Deca'; font-size: xx-large ">Tender</h1>

<p align="center" style="font-family: 'Lexend Deca'">
<strong>SOEN 343 - Council_HI</strong> <br>
Concordia University, Fall 2026
</p>


---

## Introduction
Tender is an online platform for ordering, managing and delivering food that features a Tinder-style swipe-to-match interface that helps users discover, narrow down, and agree on their perfect meal before placing an order.



---

## Docker setup

Install Docker with Docker Compose v2 and start its container runtime. From the
repository root, copy `.env.example` to `.env` and fill in `DB_URL`, `DB_USER`,
and `DB_PASSWORD` for your existing PostgreSQL or Supabase database. Also set
`PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`, and `EXPO_PUBLIC_GOOGLE_MAPS_API_KEY`.
Compose reports missing required values before building or starting. For Supabase,
use its JDBC connection details and `sslmode=require`. Database tables must match
the application schema; Docker does not apply the demo seed at startup. The
operator dashboard can reset to the demo baseline after confirmation. Set
`APP_OPERATOR_RESET_ENABLED=false` to disable that action.

On macOS with Colima, run `colima start` first. If your installation provides
`docker-compose` instead of `docker compose`, use that spelling for the commands
below.

```sh
cp .env.example .env
docker compose up --build
```

Open http://localhost:8081 for the web app. The backend is available at
http://localhost:8080. Both images install their own build dependencies; local
Java, Maven, Node, and npm installations are unnecessary. This runs the web app;
native iOS and Android development still uses Expo separately.

The root `.env` configures Compose. Database credentials are passed only to the
backend at runtime, together with the PayPal client ID and secret. PayPal uses
its sandbox API by default; log in with sandbox buyer accounts during the demo.
`EXPO_PUBLIC_GOOGLE_MAPS_API_KEY` enables Maps and Places;
restrict this browser key to your web origin and required Google APIs. The map
ID is optional. Public frontend variables are embedded during the image build,
so rebuild after changing them. No `.env` files enter either build context.

`EXPO_PUBLIC_API_URL` must be reachable from the browser. Keep `localhost` for
local use; for access from another computer, set it to the Docker host's address.
If changing `BACKEND_PORT` or `FRONTEND_PORT`, also update the public API URL and
`CORS_ALLOWED_ORIGINS` to match. Database hosts refer to addresses reachable from
the backend container; use `host.docker.internal` for a database on the Docker
Desktop host rather than `localhost`.

```sh
docker compose ps
docker compose logs -f
docker compose down
```

Startup and shutdown preserve database records. Automatic startup seeding is
disabled. The operator's confirmed Reset action restores the demo baseline unless
`APP_OPERATOR_RESET_ENABLED=false`. The existing
database stays external to Compose and is not stopped by `docker compose down`.
Use a dedicated demo database when preparing demo scenarios.

References: [Docker Compose configuration](https://docs.docker.com/reference/compose-file/)
and [Expo web serving](https://docs.expo.dev/router/web/server-rendering/).


## Run the demo on another machine

Copy the repository and a privately shared root `.env`, then run:

```sh
docker compose --env-file .env up -d --build --wait
```

On this Mac, use `docker-compose` if `docker compose` is unavailable. On the demo
computer, open `http://localhost:8081`. Docker and outbound access to Supabase,
Google Maps/Places, and PayPal sandbox are required. Restrict the Maps browser key
to the demo web origin, for example `http://localhost:8081/*`. Fill in the three
missing Maps/PayPal values before building. The `.env` contains private backend
credentials and stays outside Git and both image build contexts.

To prepare images in advance instead of building at the demo:

```sh
docker compose --env-file .env build
docker image save -o tender-demo.tar tender-backend:demo tender-frontend:demo
```

Transfer `tender-demo.tar`, `compose.yaml`, and the private `.env`. On the demo
machine:

```sh
docker image load -i tender-demo.tar
docker compose --env-file .env up -d --no-build --wait
```

These names use the default `TENDER_IMAGE_TAG=demo`. Build images for the demo
machine's CPU architecture. For an Intel/AMD demo machine when building on Apple
Silicon, use `DOCKER_DEFAULT_PLATFORM=linux/amd64 docker compose --env-file .env build`.
Images built for a different architecture need compatible emulation. Backend
secrets can change at runtime; API and Maps settings are baked into the frontend,
so rebuild that image if they change. `localhost` works when the demo browser is
on the Docker host; for other devices, use the host's reachable address for
`EXPO_PUBLIC_API_URL` and add the frontend origin to `CORS_ALLOWED_ORIGINS`.

## Backend Setup Instructions

- Spring Boot setup

1. You need the spring boot tools extension and java 17, maven for java extension is also recommended

- Database connection setup

1. You should see a .env.example file in the "backend" folder
2. copy that into a .env file created AT THE SAME LEVEL and replace the necessary (DB user and password)
3. run the app from TenderappApplication.java: if you see "Started TenderappApplication in xxx seconds" then it worked

## Order refunds

Operators can refund a delivered order from the operator dashboard. A successful
refund changes the order status to Refunded and credits its customer with
Tender Cash for future purchases; refunds are not processed through PayPal.
Each refund is recorded as a separate ledger entry, preserving the original
payment entry for traceability. Orders that are not delivered, or have already
been refunded, cannot be refunded again.

The backend endpoint is `POST /api/operator/orders/{orderId}/refund` and
requires an authenticated operator session. Backend tests can be run from
`tender-app/backend` with `./mvnw test`; frontend tests can be run from
`tender-app/frontend` with `npm test`.

## Demo test account credentials

Customer account: alice@demo.test
Courier account: leo@demo.test
Restaurant Account (Maple & Main): sam@demo.test
Operator Account: operator@demo.test

Password is "DemoPass123!" for all of them

## Paypal mock account

email: e@demo-tender.com
password: Demo123!