ALTER TABLE payment_attempts
    ADD COLUMN IF NOT EXISTS mpesa_receipt_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS result_code INTEGER,
    ADD COLUMN IF NOT EXISTS result_desc TEXT,
    ADD COLUMN IF NOT EXISTS processed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_payment_attempts_receipt
    ON payment_attempts(mpesa_receipt_number);

