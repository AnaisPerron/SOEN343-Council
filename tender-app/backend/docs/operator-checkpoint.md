# Phase II operator checkpoint

Branch: `feature/operator-backend`.

## Scope

The Phase II instructions, sections 5.2 and 5.4, require REG (R-REG-01 through
R-REG-03), ORD (R-ORD-01 through R-ORD-05), and enough dashboard functionality
to demonstrate them. Pages 6 and 7 exclude DLV and the full role-scoped command
surface. Appendix A lists broader role permissions, not a requirement to build
every operator action for this phase.

The operator focus for this checkpoint is predefined login, access to all orders,
restaurant pause/reopen, and role restrictions. The exact Moodle REG/ORD text
still needs checking before declaring the complete phase requirements satisfied.

## Implemented

- Existing BCrypt login accepts predefined operator accounts. Public registration
  cannot create operators.
- Operator API paths and services require an operator session. Operator pages
  redirect other roles and signed-out users.
- Operator login opens the dashboard and displays the authenticated account.
- Read endpoints provide full summary counts and paginated restaurants, orders,
  customers, and couriers. Account lists support the existing dashboard.
- Restaurant status updates accept open, paused, or closed. They preserve existing
  orders and other restaurant fields. Paused/closed restaurants fail the existing
  new-order availability check.
- Customer cancellation retains its ownership check and placed-only transition.
- Customer cancellation records its event transactionally and notifies observers
  after commit. Rollback and concurrent cancellation are tested.

## Removed or deferred

- All uncommitted courier reassignment files and edits were removed.
- Operator cancellation endpoints, API helpers, and eligibility helpers were removed.
  Customer cancellation and its transaction/concurrency coverage remain.
- Courier assignment, courier pickup/delivery, billing-ledger infrastructure, and
  initial-state restoration are deferred. No reset endpoint exists.
- Committed history was preserved. This checkpoint does not rewrite earlier work.

## UI integration and remaining verification

- Operator dashboard lists and full counts now come from authenticated backend
  endpoints, with refresh, pagination, empty states, and errors. Missing balances
  or availability are displayed as unknown, and order totals come from the backend.
- Restaurant pause/reopen controls now call the backend and refresh after success.
  Duplicate submissions are blocked, and expired sessions return to login.
- Central command/error logging now covers API requests, security rejections, and
  asynchronous errors without recording credentials. See [command-logging.md](command-logging.md).
  A manual UI rehearsal and role-permission verification remain.
- Billing uses a mock payment gateway, not a persistent billing ledger.

## Database and seed

- Backend timestamps map to the existing `orders.created_at` column. Java/API
  fields remain named `dateOrdered`.
- Supabase's order status constraint was expanded to accept `refunded`. This was
  a constraint-only migration and did not update order rows.
- The local seed reflects the inspected Supabase schema: no menu name, existing
  opening_hour/closing_hour columns, and an operator_profiles row for Olivia.
- Seed image URLs use the backend at localhost:8080. Other devices need a reachable
  backend origin.
- The seed remains on standby and has not been executed. It resets fifteen tables
  if run; do not run it against shared data as part of the remaining work.
- No new Supabase changes were made for this rollback/checkpoint.

## Validation

Validation passed after merging dev: 158 backend tests in a clean Maven run against H2
and mocks, 100 frontend tests, TypeScript checking, and a successful web export. Supabase and the demo seed were not
used during verification.

## Next steps

1. Recheck customer browsing and operator pause/reopen together after the dev merge.
   Customer browsing now uses the backend catalog and shows only open restaurants.
   Reload the customer page after changing a restaurant status.
2. Review and push the development branch when requested.

The UI API tests mock network responses; no pause/reopen request was sent to
Supabase during implementation. Restart the backend with the latest branch code
without enabling the demo profile before manually testing the integration.

## Dev integration

Merged dev at `5512bcc` into the operator backend branch while retaining operator
authorization, dashboard controls, customer cancellation, and command logging.
Restaurant accounts route to the new restaurant screens. Authentication details
carry the full session required by those controllers. Operator totals now use the
stored total first, matching the shared order calculation including add-ons.
A missing restaurant item photo storage helper was added to complete the imported
menu screen. No seed or database reset was run.
