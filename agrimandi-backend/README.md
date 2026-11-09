# AgriMandi AI - Backend

## Project Overview
AgriMandi AI Backend is a web-based operational management system for physical APMC mandi auctions. It manages farmers, buyers, commodities, lots, auctions, transactions, payments, and assets/crates using a robust relational schema. 

This foundation serves as the core backend for the React 19 frontend and implements all core business rules for the auction lifecycle.

## Architecture
- **Framework:** Spring Boot 3.4.x (Java 21)
- **Database:** PostgreSQL
- **Schema Management:** Flyway Migrations
- **ORM:** Spring Data JPA / Hibernate
- **Build Tool:** Maven

## Prerequisites
- Java 21+
- Maven 3.8+
- PostgreSQL 15+

## Setup & Running

### Environment Variables
The application requires the following environment variables (or rely on defaults in `application.yml`):
- `DB_URL`: JDBC URL for PostgreSQL (e.g., `jdbc:postgresql://localhost:5432/agrimandi`)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `SPRING_PROFILES_ACTIVE`: Use `dev` for development logging.

### PostgreSQL Setup (Docker)
A `docker-compose.yml` is provided to spin up a local PostgreSQL instance:
```bash
docker-compose up -d
```

### Flyway Migrations
Flyway is configured to run automatically on startup (`spring.flyway.enabled=true`). The schema and seed data are located in `src/main/resources/db/migration`.
Migrations include:
- `V1` to `V9`: Core schema (markets, users, farmers, buyers, commodities, lots, weighments, auctions, transactions, payments, assets, audit logs)
- `V10`: Seed data with a demo market, agent, farmers, buyers, commodities, and asset types.

### Running the Application
To run the backend locally:
```bash
mvn spring-boot:run
```
The application will start on port `8080`.

## API Documentation
OpenAPI (Swagger) documentation is generated automatically.
When the application is running, navigate to:
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`

## Important Business Rules Implemented
1. **Highest Bid Detection:** Handled gracefully in `AuctionService`.
2. **Tie Handling:** A tie for the highest bid sets the auction to `TIE_REQUIRES_AGENT_DECISION`. The system does NOT auto-select a winner.
3. **Winner Confirmation:** Only the highest valid bid can be confirmed. Validates state transition before creating a `Transaction`.
4. **Calculations:** Transaction `gross_amount` and `net_amount` are calculated server-side based on `quantity` * `winningRate`.
5. **Payment Validation:** Payments cannot exceed the pending outstanding balance.

## Security Architecture
The application uses **Spring Security** with **Stateless JWT Authentication**. 

- **Roles & Permissions:** 
  - `ADMIN`: Global oversight, user management.
  - `AGENT`: Primary operational user (creates lots, manages auctions, records bids). Market-scoped.
  - `FARMER`: Read-only access to their own lots and transactions.
  - `BUYER`: Read-only access to their won lots and transactions.
- **Ownership rules:** `SecurityUtils` ensures that a Farmer can only access their own records (via `farmerId` link), and a Buyer can only access their own records (via `buyerId` link).
- **Market Isolation:** `AGENT` operations are strictly constrained to their assigned `marketId` present in their JWT token.
- **Audit Trails:** `createdBy` and `recordedBy` are populated implicitly from the authenticated security context.

### Authentication Endpoints
- `POST /api/auth/login` (Accepts `username` and `password`, returns JWT)
- `GET /api/auth/me` (Returns current user info from JWT)
- `POST /api/auth/logout` (Stateless, mostly client-side)

### Demo Accounts
Demo accounts are seeded in Flyway `V11`. Passwords are all `password` (hashed with BCrypt).
- `admin.demo` (Role: ADMIN)
- `agent.demo` (Role: AGENT)
- `farmer.demo` (Role: FARMER)
- `buyer.demo` (Role: BUYER)

## Testing
Run the unit tests (Mockito based) and MockMvc security tests:
```bash
mvn test
```
*Note: Full integration tests with Testcontainers can be added; ensure the local Docker daemon is running.*
