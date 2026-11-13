-- Close assignments left active after an order finished, preserving their history.
-- Run once after deploying the delivery/refund cleanup fix. Safe to run again.
BEGIN;

-- Match the application's courier-first locking order to pause concurrent dispatch.
SELECT user_id FROM courier_profiles ORDER BY user_id FOR UPDATE;

CREATE TEMP TABLE stale_assignment_repairs ON COMMIT DROP AS
SELECT a.id AS assignment_id, a.order_id, a.courier_id
FROM assignments a JOIN orders o ON o.id = a.order_id
WHERE a.status IN ('offered', 'accepted')
  AND o.status IN ('delivered', 'refunded', 'cancelled', 'rejected');

SELECT o.id FROM orders o
WHERE o.id IN (SELECT order_id FROM stale_assignment_repairs)
ORDER BY o.id FOR UPDATE;

UPDATE assignments a
SET status = CASE WHEN o.status = 'delivered' AND a.accepted_at IS NOT NULL
                  THEN 'completed' ELSE 'ended' END
FROM orders o, stale_assignment_repairs repair
WHERE a.id = repair.assignment_id AND o.id = a.order_id;

UPDATE courier_profiles courier SET status = 'idle'
WHERE courier.user_id IN (SELECT courier_id FROM stale_assignment_repairs)
  AND courier.status IN ('assigned', 'carrying')
  AND NOT EXISTS (
      SELECT 1 FROM assignments a WHERE a.courier_id = courier.user_id
      AND a.status IN ('offered', 'accepted'))
  AND NOT EXISTS (
      SELECT 1 FROM orders o WHERE o.courier_id = courier.user_id
      AND o.status IN ('ready', 'picked_up'));

SELECT a.id AS repaired_assignment_id, a.order_id, a.courier_id, a.status
FROM assignments a JOIN stale_assignment_repairs repair ON repair.assignment_id = a.id
ORDER BY a.id;

COMMIT;
