-- Enable UUID generation (Postgres 15 usually supports gen_random_uuid via pgcrypto)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- Tenants
-- =========================
CREATE TABLE tenants (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         name VARCHAR(255) NOT NULL,
                         api_key_hash VARCHAR(255) NOT NULL UNIQUE,
                         status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                         webhook_url VARCHAR(500),
                         webhook_secret VARCHAR(255),
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_api_key_hash ON tenants(api_key_hash);

-- =========================
-- Products
-- =========================
CREATE TABLE products (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          tenant_id UUID NOT NULL REFERENCES tenants(id),
                          sku VARCHAR(100) NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          price_minor INTEGER NOT NULL,
                          currency VARCHAR(3) NOT NULL DEFAULT 'KES',
                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT uq_products_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_products_tenant_id ON products(tenant_id);
CREATE INDEX idx_products_active ON products(is_active);

-- =========================
-- Users
-- =========================
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       tenant_id UUID NOT NULL REFERENCES tenants(id),
                       external_user_id VARCHAR(255) NOT NULL,
                       phone_number VARCHAR(20),
                       email VARCHAR(255),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT uq_users_tenant_external_user UNIQUE (tenant_id, external_user_id)
);

CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_phone_number ON users(phone_number);

-- =========================
-- Orders
-- =========================
CREATE TABLE orders (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        tenant_id UUID NOT NULL REFERENCES tenants(id),
                        user_id UUID NOT NULL REFERENCES users(id),
                        product_id UUID NOT NULL REFERENCES products(id),
                        order_number VARCHAR(50) NOT NULL,
                        status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
                        total_minor INTEGER NOT NULL,
                        currency VARCHAR(3) NOT NULL DEFAULT 'KES',
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT uq_orders_tenant_order_number UNIQUE (tenant_id, order_number)
);

CREATE INDEX idx_orders_tenant_id ON orders(tenant_id);
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);

-- =========================
-- Payment Attempts
-- =========================
CREATE TABLE payment_attempts (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  order_id UUID NOT NULL REFERENCES orders(id),
                                  provider VARCHAR(50) NOT NULL DEFAULT 'MPESA',
                                  status VARCHAR(50) NOT NULL DEFAULT 'INITIATED',
                                  amount_minor INTEGER NOT NULL,
                                  currency VARCHAR(3) NOT NULL DEFAULT 'KES',
                                  phone_number VARCHAR(20),

    -- MPesa identifiers
                                  merchant_request_id VARCHAR(255),
                                  checkout_request_id VARCHAR(255),
                                  mpesa_receipt_number VARCHAR(50),

                                  response_code VARCHAR(10),
                                  response_description TEXT,

                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Idempotency/lookup indexes
CREATE INDEX idx_payment_attempts_order_id ON payment_attempts(order_id);
CREATE UNIQUE INDEX uq_payment_attempts_checkout_request_id
    ON payment_attempts(checkout_request_id)
    WHERE checkout_request_id IS NOT NULL;

CREATE UNIQUE INDEX uq_payment_attempts_mpesa_receipt_number
    ON payment_attempts(mpesa_receipt_number)
    WHERE mpesa_receipt_number IS NOT NULL;

CREATE INDEX idx_payment_attempts_merchant_request_id ON payment_attempts(merchant_request_id);

-- =========================
-- Entitlements
-- =========================
CREATE TABLE entitlements (
                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              tenant_id UUID NOT NULL REFERENCES tenants(id),
                              user_id UUID NOT NULL REFERENCES users(id),
                              order_id UUID NOT NULL REFERENCES orders(id),
                              product_id UUID NOT NULL REFERENCES products(id),
                              status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                              granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              expires_at TIMESTAMP,
                              metadata JSONB,
                              CONSTRAINT uq_entitlements_order UNIQUE (order_id)
);

CREATE INDEX idx_entitlements_tenant_id ON entitlements(tenant_id);
CREATE INDEX idx_entitlements_user_id ON entitlements(user_id);
CREATE INDEX idx_entitlements_status ON entitlements(status);

-- =========================
-- Webhook Deliveries
-- =========================
CREATE TABLE webhook_deliveries (
                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                    tenant_id UUID NOT NULL REFERENCES tenants(id),
                                    order_id UUID NOT NULL REFERENCES orders(id),
                                    event_type VARCHAR(100) NOT NULL,
                                    payload JSONB NOT NULL,
                                    delivery_url VARCHAR(500) NOT NULL,
                                    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                                    attempts INTEGER NOT NULL DEFAULT 0,
                                    last_attempt_at TIMESTAMP,
                                    response_code INTEGER,
                                    response_body TEXT,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_webhook_deliveries_tenant_id ON webhook_deliveries(tenant_id);
CREATE INDEX idx_webhook_deliveries_status ON webhook_deliveries(status);
CREATE INDEX idx_webhook_deliveries_order_id ON webhook_deliveries(order_id);
