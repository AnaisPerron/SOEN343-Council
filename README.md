<h1 align="center" style="font-family: 'Lexend Deca'; font-size: xx-large ">Tender</h1>

<p align="center" style="font-family: 'Lexend Deca'">
<strong>SOEN 343 - Council_HI</strong> <br>
Concordia University, Fall 2026
</p>


---

## Introduction
Tender is an online platform for ordering, managing and delivering food that features a Tinder-style swipe-to-match interface that helps users discover, narrow down, and agree on their perfect meal before placing an order.



---

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