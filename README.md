# AgriMandi AI — APMC Operations Platform

AgriMandi AI is a full-stack operational management platform for physical APMC (Agricultural Produce Market Committee) mandi auctions. It manages farmers, buyers, commodities, lots, weighments, live floor auctions, transaction settlements, payment ledgers, and returnable packaging asset accounting.

---

## 🏛️ Architecture Overview

```
├── agrimandi-ai/           # Frontend: React 19, TypeScript, Vite, Tailwind CSS, Lucide icons, Recharts
├── agrimandi-backend/      # Backend: Spring Boot 3.4 (Java 21), Spring Security (JWT), Spring Data JPA, Flyway
├── stitch_agrimandi_ai_apmc_operations_platform/ # Original UI design specs and HTML screens
├── AgriMandi_AI_Full_Stack_Engineering_Spec_v1.md # Full-stack engineering specification
└── frontend_reality_audit.md # Complete audit of screens, APIs, security, and verification
```

### Backend (`agrimandi-backend`)
- **Runtime:** Java 21+ / Spring Boot 3.4
- **Database:** PostgreSQL with automated Flyway schema migrations
- **Security:** Stateless JWT authentication with role-based access control (`ADMIN`, `AGENT`, `FARMER`, `BUYER`) and market-scoped isolation
- **Business Rules:** Server-side net weight computations, auction bidding with tie detection, statutory takpatti bill calculations, payment limits, and crate inventory tracking

### Frontend (`agrimandi-ai`)
- **Runtime:** Node.js 20+ / React 19 / Vite
- **Styling & UI:** Tailwind CSS, responsive APMC operational layouts
- **Routing:** React Router v7 with role-based landing redirection

---

## 🚀 Getting Started

### 1. Database Setup (PostgreSQL)
Ensure PostgreSQL is running locally on port `5432`:
```sql
CREATE DATABASE agrimandi;
```
*(Default credentials expected: username `postgres`, password `postgres`)*

### 2. Run the Backend
```bash
cd agrimandi-backend
mvn spring-boot:run
```
The backend starts on `http://localhost:8080`. Flyway migrations run automatically on startup.

### 3. Run the Frontend
```bash
cd agrimandi-ai
npm install
npm run dev
```
The web application is accessible at `http://localhost:5173`.

---

## 🔑 Demo Accounts

All demo accounts use password: `password`

| Username | Role | Landing Route | Description |
| :--- | :--- | :--- | :--- |
| `agent.demo` | **AGENT** | `/dashboard` | Operational mandi agent (creates lots, conducts auctions, issues bills) |
| `admin.demo` | **ADMIN** | `/admin/dashboard` | APMC Secretary / Admin oversight & audit trail |
| `farmer.demo` | **FARMER** | `/farmer-portal` | Farmer digital passbook (own lots and receipts) |
| `buyer.demo` | **BUYER** | `/buyer-portal` | Licensed buyer portal (won lots, outstanding balance, asset crates) |

---

## 🧪 Testing

### Backend Tests
```bash
cd agrimandi-backend
mvn clean test
```

### Frontend Build & Lint
```bash
cd agrimandi-ai
npm run build
npm run lint
```
