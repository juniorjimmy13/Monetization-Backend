ALTER TABLE tenants
    ADD COLUMN api_key_hash_sha256 VARCHAR(64),
    ADD COLUMN api_key_hash_bcrypt VARCHAR(255);

-- Make bcrypt the real storage, and sha256 the lookup
-- (You can drop api_key_hash later in V3 once you're stable.)
CREATE INDEX idx_tenants_api_key_sha256 ON tenants(api_key_hash_sha256);
