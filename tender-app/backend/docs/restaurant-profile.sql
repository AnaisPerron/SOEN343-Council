-- Apply to an existing PostgreSQL database before deploying restaurant contact profiles.
ALTER TABLE public.restaurants ADD COLUMN IF NOT EXISTS phone varchar(20);
