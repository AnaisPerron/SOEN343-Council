BEGIN;

SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '30s';

ALTER TABLE public.orders
    DROP CONSTRAINT chk_order_status,
    ADD CONSTRAINT chk_order_status CHECK (status IN (
        'placed', 'accepted', 'preparing', 'ready', 'picked_up',
        'delivered', 'cancelled', 'rejected', 'refunded'
    ));

COMMIT;
