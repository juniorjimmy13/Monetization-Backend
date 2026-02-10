ALTER TABLE payment_attempts
    ADD COLUMN provider_reference VARCHAR(255);

-- Backfill existing rows (if any) so we can enforce NOT NULL safely
UPDATE payment_attempts
SET provider_reference = COALESCE(provider_reference, gen_random_uuid()::text);

ALTER TABLE payment_attempts
    ALTER COLUMN provider_reference SET NOT NULL;

-- Add unique constraint (matches your entity)
ALTER TABLE payment_attempts
    ADD CONSTRAINT uq_payment_attempts_provider_reference UNIQUE (provider_reference);
