BEGIN;

CREATE TABLE public.assignments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES public.orders(id),
    courier_id BIGINT NOT NULL REFERENCES public.courier_profiles(user_id),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL DEFAULT 'offered',
    CONSTRAINT chk_assignment_status
        CHECK (status IN ('offered', 'accepted', 'completed', 'ended')),
    CONSTRAINT chk_assignment_timestamps
        CHECK (accepted_at IS NULL OR accepted_at >= assigned_at),
    CONSTRAINT chk_assignment_acceptance
        CHECK (
            (status = 'offered' AND accepted_at IS NULL)
            OR (status IN ('accepted', 'completed') AND accepted_at IS NOT NULL)
            OR status = 'ended'
        )
);

CREATE UNIQUE INDEX uq_assignment_active_order
    ON public.assignments (order_id)
    WHERE status IN ('offered', 'accepted');

CREATE UNIQUE INDEX uq_assignment_active_courier
    ON public.assignments (courier_id)
    WHERE status IN ('offered', 'accepted');

CREATE INDEX idx_assignments_order_history
    ON public.assignments (order_id, assigned_at DESC, id DESC);

CREATE INDEX idx_assignments_courier_history
    ON public.assignments (courier_id, assigned_at DESC, id DESC);

ALTER TABLE public.assignments ENABLE ROW LEVEL SECURITY;

COMMIT;
