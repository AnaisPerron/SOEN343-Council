# R-PRC-01 Pricing rules

The backend reads these values from `tender-app/backend/src/main/resources/application.properties`:

| Rule | Configuration key | Value |
| --- | --- | ---: |
| Delivery fee per straight-line kilometre | `app.pricing.delivery-fee-per-km` | $0.80/km |
| Service fee per order | `app.pricing.service-fee` | $1.50 |
| Small-order threshold (items subtotal) | `app.pricing.small-order-threshold` | $15.00 |
| Small-order surcharge when subtotal is below the threshold | `app.pricing.small-order-surcharge` | $2.00 |

The items subtotal is the sum of each ordered quantity multiplied by its menu price plus selected add-on prices. The backend calculates the straight-line (Haversine) distance between the restaurant and the selected delivery address, rounds kilometres to two decimals using half-up rounding, multiplies by $0.80, then rounds the delivery fee to cents using half-up rounding. The surcharge applies only when the items subtotal is **strictly below** $15.00. Each order is priced separately, including a cart containing multiple restaurants. The charged amount, stored order total, and bill all use the same calculation. Order address and location are snapshotted at placement, so later profile changes do not change that order's fare.

