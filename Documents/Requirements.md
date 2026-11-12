<!--
  This file is mostly used as a reference to refer back to.
  It lists the requirements only. I, Anais Perron, certify that Claude generated this file from the Project Description 
-->

#  Requirements of the System
 
## Mandatory modules
 
### REG – Registration, Login & Profiles
 
| ID | Requirement |
|---|---|
| R-REG-01 | REG shall create accounts holding exactly one role: customer \| restaurant \| courier \| operator. |
| R-REG-02 | REG shall require an authenticated session for every command, including browsing menus. There is no guest access. |
| R-REG-03 | REG shall load seeded accounts, at least one per role, from the configuration file so that demos start from a known state. |
 
### ORD – Order & Restaurant Core
 
| ID | Requirement |
|---|---|
| R-ORD-01 | ORD shall load a configuration file describing the initial city and display it on the map: **Restaurant** (name; status open \| paused \| closed; lat/long position; menu), **Menu Item** (id; name; price; status available \| sold_out), **Courier** (id; lat/long position; status), **Customer** (id; name; delivery address lat/long). |
| R-ORD-02 | ORD shall prevent placing an order at a paused or closed restaurant, or an order containing a sold-out item. |
| R-ORD-03 | ORD shall prepare each restaurant's accepted orders in arrival order. The queue sets the sequence of preparation only; no order is refused for want of capacity. |
| R-ORD-04 | ORD shall enforce the order lifecycle and reject any transition that skips a state; in particular, an order cannot be picked up before it is ready. |
| R-ORD-05 | ORD shall record all state transitions with an event id. |
 
### DLV – Courier & Delivery
 
| ID | Requirement |
|---|---|
| R-DLV-01 | DLV shall assign an order only after the restaurant has marked it ready. |
| R-DLV-02 | DLV shall select an idle on-duty courier and shall not reassign an accepted order except by operator command. |
| R-DLV-03 | DLV shall allow a courier to hold at most one order at a time, and shall prevent a courier from going offline while carrying one. |
| R-DLV-04 | DLV shall hold orders with no available courier in a dispatch queue, and retry assignment whenever a courier becomes idle. |
 
**Dispatch rule (fixed):** when an order becomes ready, offer it to the nearest idle on-duty courier by straight-line distance on the seeded coordinate grid, breaking ties by the lowest courier id.
 
### DASH – Simulator UI
 
| ID | Requirement |
|---|---|
| R-DASH-01 | DASH shall display only the actions permitted to the logged-in role (see below). |
| R-DASH-02 | DASH shall render all restaurants and on-duty couriers as markers positioned by lat/lon within the current dataset bounds. |
| R-DASH-03 | DASH shall encode each restaurant's preparation queue with three levels: long (red) at six or more queued orders, or when the restaurant is paused; building (amber) at three to five; short (green) otherwise. |
| R-DASH-04 | DASH shall disable Place Order when the restaurant is paused or closed, or when every item selected is sold out. |
| R-DASH-05 | DASH shall disable Confirm Pickup until the order is ready, and Confirm Delivery until it is picked_up. |
| R-DASH-06 | DASH shall apply visual updates of a successful command, log every command and error to the output console, and handle an empty map gracefully. |
 
**Permitted actions per role (R-DASH-01):**
 
- **Customer:** browse menus; place an order; cancel a placed order; view own history.
- **Restaurant Owner/Staff:** edit the menu and item availability; accept or reject; mark ready; pause the restaurant.
- **Courier:** go on/off duty; accept an offer; confirm pickup; confirm delivery.
- **Operator:** pause or reopen any restaurant; reassign a stalled order; view all orders and ledgers; refund a delivered order; restore the initial state.
### PRC – Pricing, Billing & Order History
 
| ID | Requirement |
|---|---|
| R-PRC-01 | PRC shall compute the cost deterministically from the pricing rules and the order facts. |
| R-PRC-02 | PRC shall provide an order summary after delivery. |
| R-PRC-03 | PRC shall maintain a ledger of all orders and charges, holding for each order its final state, timestamps and total charge. |
| R-PRC-04 | PRC shall scope history by role: a customer sees only their own orders, a restaurant only its own, a courier only the deliveries they carried, and an operator all of them. |
| R-PRC-05 | PRC shall let an operator refund a delivered order, recording the refund in the ledger as a separate entry that leaves the original charge unchanged; the order moves to refunded. |
 
**Fare:** sum of the order lines, plus a delivery fee and a service fee, with an optional small-order surcharge.
 
## Optional modules
 
### NTF – Notifications
 
| ID | Requirement |
|---|---|
| R-NTF-01 | NTF shall notify a customer whenever their order changes state. |
| R-NTF-02 | NTF shall alert the operator when an order has waited in the dispatch queue beyond a configured threshold. |
 
### RVW – Ratings & Reviews
 
| ID | Requirement |
|---|---|
| R-RVW-01 | RVW shall accept at most one rating per delivered order, and only from the customer who placed it. |
| R-RVW-02 | RVW shall display a restaurant's average rating on its details panel. |
 
### PROMO – Promotions
 
| ID | Requirement |
|---|---|
| R-PROMO-01 | PROMO shall apply at most one promo code per order, validated at placement time. |
| R-PROMO-02 | PROMO shall reject expired codes and codes below their minimum order value, with a clear message. |
