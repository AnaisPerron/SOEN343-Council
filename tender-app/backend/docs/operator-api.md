# Operator backend integration

This document records the contracts for `feature/operator-backend`. Proposed
routes below are not implemented yet. The dashboard still displays mock data;
API helper tests mock HTTP responses and do not prove those routes exist.

## Existing endpoints to reuse

| Endpoint | Current behavior |
| --- | --- |
| `POST /api/auth/login` | Validates a predefined user's BCrypt password and creates a session. |
| `GET /api/auth/session` | Restores the account for a valid bearer token. |
| `GET /api/profile` | Returns the authenticated user's profile. |
| `PATCH /api/profile` | Updates name and phone; both fields are required. |
| `POST /api/orders` | Customer-only order placement. Operators must remain forbidden. |
| `POST /api/orders/{id}/cancel` | Customer-only cancellation of that customer's placed order. |

Profile update request:

```json
{ "name": "Olivia Operator", "phone": "+15145550108" }
```

Profile responses use `id`, `name`, `email`, `phone`, and an uppercase role
(`OPERATOR` for an operator). Restaurant metadata may also be present for staff.
The operator helper preserves the current name when changing the phone, sends
PATCH using the shared API client, and refuses to relabel other roles as operators.
Password changes are not supported by `UpdateProfileRequest` or `ProfileController`.
The frontend must not report them as successful or silently send an ignored field.

## Operator endpoints to implement

The security filter now requires the existing bearer session with
`ROLE_OPERATOR` for all `/api/operator/**` routes. Endpoint implementations are still pending. No caller-supplied role or customer ID can grant access.
Return 401 for missing/invalid/expired tokens, 403 for other account roles,
400 for invalid inputs, 404 for missing targets, and 409 for state conflicts.
Reuse the existing error response conventions (`message`, optional `fieldErrors`).
Do not return password hashes or session tokens in dashboard data.

### Dashboard reads

| Proposed endpoint | Data |
| --- | --- |
| `GET /api/operator/summary` | Restaurant, order, customer, courier, and available-courier counts. |
| `GET /api/operator/restaurants` | Restaurant IDs, names, status, coordinates, and available contact details. |
| `GET /api/operator/orders` | All restaurants' order IDs, participant IDs, status, timestamps, and totals. |
| `GET /api/operator/customers` | Customer contact details, coordinates, and TD Cash balance when a profile exists. |
| `GET /api/operator/couriers` | Courier contact details, coordinates, and availability status. |

List endpoints should use `page` (zero-based) and `size` (default 50, maximum 100),
with stable ID ordering and an envelope containing `items`, `total`, `page`, and
`size`. Summary counts cover the complete result sets, not just the loaded page.
Treat only `idle` couriers as available. Missing profile data must be shown as
unknown rather than fabricated from frontend fixtures.

Use camelCase response DTO fields and lowercase status values. Adapt the existing
snake_case mock view models explicitly when connecting the dashboard. Handle
nullable IDs/timestamps and every persisted order state, including `refunded`.
Order totals must follow the shared backend calculation, not mock order lines.

### Restaurant status

Retain the teammate's helper contract:

```http
PUT /api/operator/restaurants/{restaurantId}/status
Authorization: Bearer <token>
Content-Type: application/json

{ "status": "paused" }
```

Allow only `open`, `paused`, or `closed`. Return 204 on success; the client refreshes
the list and summary. Setting the same state can be an idempotent success.
Preserve existing orders. Reuse `BrowseService.checkRestaurantOpen` for new-order
restrictions; do not bypass these restrictions through the operator UI.

### Operator cancellation

Retain the teammate's helper contract:

```http
POST /api/operator/orders/{orderId}/cancel
Authorization: Bearer <token>
Content-Type: application/json

{}
```

Return 204 on success. Initially permit only `placed -> cancelled`, matching the
existing `OrderStatus` state machine and customer cancellation rule. Return 409
for other states. The frontend eligibility helper follows that same restriction.
Broader administrative cancellation requires an explicit state/refund policy
before changing this rule.

An operator may target another customer's order, so do not call the customer
controller or pass the operator ID as a customer ID. Reuse shared transactional
logic: lock the order, validate state, transition through the entity, and record
the cancellation via `PreparationQueueService`. Preserve customer ownership
checks on the existing customer endpoint. Evaluate observer notifications when
integrating this action so restaurant views do not retain stale queue entries.

## Remaining requirements and prerequisites

- Operator login and restored sessions now route to `/operator/dashboard`. The
  operator layout waits for session restoration and redirects other roles or
  signed-out users. Dashboard identity comes from the authenticated account.
  Public registration rejects operator accounts; predefined accounts still log
  in through the existing BCrypt/session flow. Dashboard lists remain mocked.
- Restaurant preparation and delivery endpoints must remain unavailable to
  operators. Existing restaurant order routes require a matching staff account.
- Courier reassignment needs an offline current courier, an eligible replacement,
  a defined eligible order state, and transaction/locking rules. Order already
  maps `courier_id`; courier profiles currently have no dedicated backend entity
  on this branch. Define the request/response after inspecting that model.
- Billing currently uses `MockPaymentGateway`: it returns a generated payment
  reference and logs approval, but provides no persistent billing ledger or
  refund operation. Do not present mock totals as a billing ledger. Define its
  schema and migration before implementing ledger reads or refund accounting.
- Restoration needs an explicit initial-state definition, table/dependency scope,
  environment restrictions, and confirmation behavior before exposing an endpoint.
  It must be tested only in a disposable database. Do not execute the demo seed
  or any restoration against the configured Supabase database during development.

## Integration sequence

1. Complete branch setup and dependency/test checks (done).
2. Align existing profile/cancellation helper contracts and record missing APIs.
3. Add operator backend authorization and login routing (done).
4. Implement live dashboard reads and remove mock dependencies.
5. Implement restaurant pause/reopen.
6. Implement operator cancellation through shared order logic.
7. Implement validated courier reassignment.
8. Define and implement billing-ledger storage and reads.
9. Define and implement restoration in an isolated test environment.
10. Verify full operator flows and forbidden customer/staff/courier operations.
