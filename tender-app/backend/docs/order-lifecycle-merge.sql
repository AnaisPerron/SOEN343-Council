-- Apply to an existing PostgreSQL database before deploying the merged order lifecycle.
-- The current schema snapshot has no date_ordered column and rejects refunded orders.
BEGIN;

ALTER TABLE public.orders
    ADD COLUMN IF NOT EXISTS date_ordered timestamp without time zone;

UPDATE public.orders
SET date_ordered = COALESCE(accepted_at AT TIME ZONE 'UTC', CURRENT_TIMESTAMP AT TIME ZONE 'UTC')
WHERE date_ordered IS NULL;

ALTER TABLE public.orders
    ALTER COLUMN date_ordered SET NOT NULL;

ALTER TABLE public.orders
    DROP CONSTRAINT IF EXISTS chk_order_status;

ALTER TABLE public.orders
    ADD CONSTRAINT chk_order_status CHECK (status IN (
        'placed', 'accepted', 'preparing', 'ready', 'picked_up',
        'delivered', 'cancelled', 'rejected', 'refunded'
    ));

COMMIT;
