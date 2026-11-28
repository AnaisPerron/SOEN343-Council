# Phase IV refactoring log

## Baseline and hotspot selection

Branch: `phase3-refactored`. Application code matched the Phase III baseline before R1; the branch also contained the SonarQube workflow and Lizard report. Hotspots below use the SonarQube CSV and issue exports committed on `main` at `e8288ad` because those exports are not on this branch. Metrics are per file, not per method. The table favors order behavior relevant to Ratings & Reviews (RVW).

| Class | NLOC | Cyclomatic | Cognitive | Sonar issues | Observed reason to inspect |
| --- | ---: | ---: | ---: | ---: | --- |
| `ord.services.OrderService` | 403 | 68 | 37 | 0 | Largest backend class; handles delivery and refund transitions plus response mapping. Delivered status will matter to rating eligibility. |
| `ord.services.CustomerOrderService` | 194 | 42 | 38 | 0 | `prepareOrder` combines address, menu, option, and price checks. Checkout structure is relevant to future order extension. |
| `ord.entities.Order` | 186 | 60 | 25 | 0 | Holds status transitions and delivery timestamps; delivered state will matter to rating eligibility. |
| `dlv.services.AssignmentService` | 132 | 52 | 40 | 0 | `current` handles lookup, consistency checks, and assignment adoption. Delivery completion depends on this service. |
| `technicalservices.payment.PayPalCheckoutService` | 168 | 46 | 43 | 2 | One reported code smell (regex performance) and one bug (interrupt handling); high complexity, but less directly related to RVW. |

## R1 — Customer order preparation

**AI proposal:** Extract the delivery-address checks, menu availability checks, and selected-option checks from `CustomerOrderService.prepareOrder` into small private methods. Preserve their order, exception types and messages, repository calls, subtotal calculation, and payment behavior. This reduces the method's mixed responsibilities without adding functionality.

**Decision:** Accept R1 as the first refactoring. The validation stages are separable and already have a public entry point through `quoteOrder` and `placeOrder`. Defer the other hotspots until R1 is verified.

### Characterization baseline, before production changes

Commit: `e58017a4 test(R1): characterize customer order preparation`.

New tests cover delivery override coordinates and option prices, invalid address overrides, coordinates without an address, and a missing saved address. Existing tests cover closed restaurants, unavailable items, wrong item or option ownership, payment, and order persistence.

| Command (from `tender-app/backend`) | Result |
| --- | --- |
| `.\mvnw.cmd '-Dtest=com.council.tenderapp.ord.services.OrderServiceTest' test` | PASS — 30 tests, 0 failures, 0 errors, 0 skipped. |
| `.\mvnw.cmd test` | PASS — 358 tests, 0 failures, 0 errors, 4 skipped. |

Both commands ran before any production code was changed.

### R1 implementation and verification

Extract Class: the whole preparation stage moved from `CustomerOrderService` into the new package-private class `ord.services.OrderPreparation` (`prepare`, `validateDeliveryOverride`, `availableMenu`, `selectedOptions`, `savedDelivery`, `paidUnitPrice`, and the `PreparedOrder` record). `CustomerOrderService` keeps payment and persistence and delegates to it. Its public constructor is unchanged; it builds `OrderPreparation` from the collaborators it already received, so the characterization tests did not change. The original validation order, exception messages, lookup arguments, subtotal calculation, and delivery selection (including the unconditional saved-address lookup) remain the same.

| Command (from `tender-app/backend`) | Result |
| --- | --- |
| `.\mvnw.cmd '-Dtest=com.council.tenderapp.ord.services.OrderServiceTest' test` | PASS — 30 tests, 0 failures, 0 errors, 0 skipped. |
| `.\mvnw.cmd -q test` | PASS — 358 tests, 0 failures, 0 errors, 4 skipped. |

No Ratings & Reviews behavior was added in R1.
