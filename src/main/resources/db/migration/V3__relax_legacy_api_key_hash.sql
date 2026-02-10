-- We no longer use api_key_hash; keep it only for backward compatibility during dev
ALTER TABLE tenants
    ALTER COLUMN api_key_hash DROP NOT NULL;
