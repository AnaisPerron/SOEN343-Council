# Phase IV refactoring log

## Baseline and hotspot selection

Branch: `phase3-refactored`. Application code matched the Phase III baseline before R1; the branch also contained the SonarQube workflow and Lizard report. The source for this selection is the **last commit of `main`**, `e8288adab36dc80897db038dbe221c9270934f35` (`metrics for phase 3 added`): `docs/metrics/phase3/sonarqube-metrics.csv` and `docs/metrics/phase3/sonarqube-all-issues.json`. These files are not present on `phase3-refactored`, so their recorded values were read directly from that commit. Metrics are per file, not per method. The five selected files rank first through fifth by cyclomatic complexity among the 180 backend production Java files in the CSV; order-related files take priority for Ratings & Reviews (RVW).

| Class | NLOC (rank) | Cyclomatic (rank) | Cognitive (rank) | Sonar issues | Observed reason to inspect |
| --- | ---: | ---: | ---: | ---: | --- |
| `ord.services.OrderService` | 403 (1) | 68 (1) | 37 (5) | 0 | Largest backend class; handles delivery and refund transitions plus response mapping. Delivered status will matter to rating eligibility. |
| `ord.services.CustomerOrderService` | 194 (2) | 42 (5) | 38 (4) | 0 | `prepareOrder` combines address, menu, option, and price checks. Checkout structure is relevant to future order extension. |
| `ord.entities.Order` | 186 (3) | 60 (2) | 25 (7) | 0 | Holds status transitions and delivery timestamps; delivered state will matter to rating eligibility. |
| `dlv.services.AssignmentService` | 132 (8) | 52 (3) | 40 (3) | 0 | `current` handles lookup, consistency checks, and assignment adoption. Delivery completion depends on this service. |
| `technicalservices.payment.PayPalCheckoutService` | 168 (5) | 46 (4) | 43 (2) | 2 | One reported code smell (regex performance) and one bug (interrupt handling); high complexity, but less directly related to RVW. |

## R1 — Customer order preparation

**AI proposal:** Extract the delivery-address checks, menu availability checks, and selected-option checks from `CustomerOrderService.prepareOrder` into small private methods. Preserve their order, exception types and messages, repository calls, subtotal calculation, and payment behavior. This reduces the method's mixed responsibilities without adding functionality.

**Decision:** Accept R1 as the first refactoring. In the last commit of `main`, `CustomerOrderService` ranks second by NLOC, fourth by cognitive complexity, and fifth by cyclomatic complexity among backend production Java files. Its validation stages are separable and already have a public entry point through `quoteOrder` and `placeOrder`. Defer the other hotspots until R1 is verified.

### Characterization baseline, before production changes

Commit: `e58017a4 test(R1): characterize customer order preparation`.

New tests cover delivery override coordinates and option prices, invalid address overrides, coordinates without an address, and a missing saved address. Existing tests cover closed restaurants, unavailable items, wrong item or option ownership, payment, and order persistence.

| Command (from `tender-app/backend`) | Result |
| --- | --- |
| `.\mvnw.cmd '-Dtest=com.council.tenderapp.ord.services.OrderServiceTest' test` | PASS — 30 tests, 0 failures, 0 errors, 0 skipped. |
| `.\mvnw.cmd test` | PASS — 358 tests, 0 failures, 0 errors, 4 skipped. |

Both commands ran before any production code was changed.

### R1 implementation and verification

Extracted `validateDeliveryOverride`, `availableMenu`, and `selectedOptions` from `prepareOrder`. The original validation order, exception messages, lookup arguments, subtotal calculation, and delivery selection remain in place.

| Command (from `tender-app/backend`) | Result |
| --- | --- |
| `.\mvnw.cmd '-Dtest=com.council.tenderapp.ord.services.OrderServiceTest' test` | PASS after recompiling 180 production source files — 30 tests, 0 failures, 0 errors, 0 skipped. |
| `.\mvnw.cmd -q test` | PASS — 358 tests, 0 failures, 0 errors, 4 skipped (from 61 Surefire XML reports). |

No Ratings & Reviews behavior was added in R1.
