# Digital Monetization Backend

Multi-tenant digital monetization system with MPesa payment integration for live-service games and digital platforms.

**Author:** Jimmy Junior Njenga (ID: 668648)  
**Institution:** United States International University - Africa  
**Project:** SWE3090A -  Project 1

## Project Overview

This backend system provides:
- **Multi-tenant architecture** - Multiple games/apps use the same infrastructure
- **Payment processing** - MPesa STK Push integration
- **Entitlement management** - Automatic ownership tracking
- **Webhook notifications** - Real-time event delivery to clients
- **Admin APIs** - Tenant and product configuration

## Architecture

```
Client Applications
        ↓
   REST APIs
        ↓
   Services Layer (Order, Payment, Entitlement)
        ↓
   PostgreSQL Database
        ↓
   MPesa Payment Provider
```

## Quick Start

### Prerequisites

- Java 17+ JDK
- PostgreSQL 15+
- Maven 3.9+
- IntelliJ IDEA (recommended) or any Java IDE

### 1. Database Setup

```bash
# Create PostgreSQL database
createdb monetization_db

# Or using psql
psql -U postgres
CREATE DATABASE monetization_db;
\q
```

### 2. Clone and Build

```bash
# Clone repository
git clone <your-repo-url>
cd monetization-backend

# Build project
./mvnw clean install

# Run database migrations
./mvnw flyway:migrate
```

### 3. Run Application

```bash
# Run Spring Boot application
./mvnw spring-boot:run

# Application will start on http://localhost:8080
```

### 4. Access API Documentation

Open browser: http://localhost:8080/swagger-ui.html

## 📊 Database Schema

### Tables
1. **tenants** - Tenant organizations (game studios)
2. **products** - Purchasable items per tenant
3. **users** - End users scoped to tenants
4. **orders** - Purchase transactions
5. **payment_attempts** - Payment provider interactions
6. **entitlements** - Digital ownership records
7. **webhook_deliveries** - Outgoing event notifications

### Entity Relationships

```
Tenant (1) ─────> (*) Products
   │
   └──> (*) Users
   │
   └──> (*) Orders ─────> (1) Product
                 │
                 ├──> (*) PaymentAttempts
                 │
                 └──> (1) Entitlement
```

## Configuration

### Environment Variables

Create `.env` file (not committed to Git):

```bash
# Database
DATABASE_URL=jdbc:postgresql://localhost:5432/monetization_db
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password

# Admin API Key
ADMIN_API_KEY=sk_admin_your_secret_key

# MPesa (Get from https://developer.safaricom.co.ke)
MPESA_CONSUMER_KEY=your_consumer_key
MPESA_CONSUMER_SECRET=your_consumer_secret
MPESA_SHORTCODE=174379
MPESA_PASSKEY=your_passkey
MPESA_CALLBACK_URL=http://your-domain.com/api/v1/webhooks/mpesa/callback
```

## API Usage Examples

### 1. Create Tenant (Admin Only)

```bash
POST /api/v1/admin/tenants
Authorization: Bearer sk_admin_your_secret_key
Content-Type: application/json

{
  "name": "Epic Game Studio",
  "webhookUrl": "https://game.example.com/webhooks",
  "webhookSecret": "webhook_secret_123"
}

Response:
{
  "tenantId": "uuid",
  "apiKey": "sk_live_abc123xyz..." // Save this! Shown only once
}
```

### 2. Create Product

```bash
POST /api/v1/admin/products
Authorization: Bearer <tenant_api_key>
Content-Type: application/json

{
  "sku": "premium-sword",
  "name": "Premium Sword",
  "description": "A legendary blade",
  "priceMajor": 100.00,
  "currency": "KES"
}
```

### 3. Create Order

```bash
POST /api/v1/orders
Authorization: Bearer <tenant_api_key>
Content-Type: application/json

{
  "externalUserId": "player123",
  "productSku": "premium-sword",
  "phoneNumber": "254712345678"
}
```

### 4. Initiate Payment

```bash
POST /api/v1/payments/initiate
Authorization: Bearer <tenant_api_key>
Content-Type: application/json

{
  "orderId": "uuid",
  "phoneNumber": "254712345678"
}
```

### 5. Query Entitlements

```bash
GET /api/v1/entitlements?externalUserId=player123
Authorization: Bearer <tenant_api_key>
```

## Testing

### Run Tests

```bash
# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

### Test Database

Tests use H2 in-memory database automatically.

## Project Structure

```
monetization-backend/
├── src/
│   ├── main/
│   │   ├── java/com/usiu/monetization/
│   │   │   ├── config/          # Configuration classes
│   │   │   ├── controller/      # REST controllers
│   │   │   ├── dto/             # Data Transfer Objects
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom exceptions
│   │   │   ├── repository/      # JPA repositories
│   │   │   ├── security/        # Security filters
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Utility classes
│   │   └── resources/
│   │       ├── db/migration/    # Flyway SQL migrations
│   │       └── application.properties
│   └── test/                    # Test classes
├── pom.xml                      # Maven dependencies
└── README.md
```

## Security

### Authentication
- API key-based authentication (Bearer token)
- Separate admin and tenant keys
- Keys stored as BCrypt hashes

### Multi-Tenancy
- All data scoped to tenant_id
- Automatic tenant isolation in queries
- Prevented cross-tenant data access

### MPesa Security
- Callback signature verification
- IP whitelisting (production)
- Amount validation
- Idempotency checks

## Deployment

### Railway.app (Recommended)

1. Connect GitHub repository
2. Add PostgreSQL service
3. Set environment variables
4. Deploy automatically on push

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Deploy
railway up
```

##  Troubleshooting

### Database Connection Issues

```bash
# Check PostgreSQL is running
pg_isready

# Check database exists
psql -U postgres -l | grep monetization_db
```

### Migration Failures

```bash
# Reset database (DANGER: deletes all data!)
./mvnw flyway:clean
./mvnw flyway:migrate
```

### Port Already in Use

```bash
# Change port in application.properties
server.port=8081
```
## Documentation

- [API Documentation](http://localhost:8080/swagger-ui.html) (when running)

## Contributing

This is an academic project. For suggestions or issues, contact:
- **Email:** jnjenga@usiu.ac.ke
- **Supervisor:** Prof. Paul Okanda

## License

This project is submitted as part of academic requirements at USIU-Africa.

## Acknowledgments

- Prof. Paul Okanda (Project Supervisor)
- USIU-Africa School of Science and Technology
- Safaricom Daraja API Team

---
